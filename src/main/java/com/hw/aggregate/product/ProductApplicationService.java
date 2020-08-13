package com.hw.aggregate.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.hw.aggregate.attribute.AppBizAttributeApplicationService;
import com.hw.aggregate.catalog.PublicCatalogApplicationService;
import com.hw.aggregate.product.command.AdminCreateProductCommand;
import com.hw.aggregate.product.command.AdminUpdateProductCommand;
import com.hw.shared.DeepCopyException;
import com.hw.aggregate.product.exception.HangingTransactionException;
import com.hw.aggregate.product.exception.ProductNotFoundException;
import com.hw.aggregate.product.exception.RollbackNotSupportedException;
import com.hw.aggregate.product.model.*;
import com.hw.aggregate.product.representation.*;
import com.hw.shared.IdGenerator;
import com.hw.shared.sql.PatchCommand;
import com.hw.shared.sql.RestfulEntityManager;
import com.hw.shared.sql.SumPagedRep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.hw.aggregate.product.representation.AdminProductRep.ADMIN_REP_SKU_LITERAL;
import static com.hw.config.AppConstant.REVOKE;
import static com.hw.shared.AppConstant.PATCH_OP_TYPE_DIFF;
import static com.hw.shared.AppConstant.PATCH_OP_TYPE_SUM;

@Slf4j
@Service
public class ProductApplicationService {

    @Autowired
    private ProductRepo repo;
    @Autowired
    private ChangeRecordRepository changeHistoryRepository;

    @Autowired
    private PublicCatalogApplicationService catalogApplicationService;

    @Autowired
    private AppBizAttributeApplicationService attributeApplicationService;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private ProductManager productDetailManager;
    @Autowired
    private ProductSkuManager productSkuManager;

    @Autowired
    private ObjectMapper om;

    @Transactional(readOnly = true)
    public AdminProductSumPagedRep readForAdminByQuery(String query, String page, String countFlag) {
        SumPagedRep<Product> pagedRep = productDetailManager.readByQuery(RestfulEntityManager.RoleEnum.ADMIN, query, page, countFlag, Product.class);
        return new AdminProductSumPagedRep(pagedRep);
    }

    @Transactional(readOnly = true)
    public PublicProductSumPagedRep readForPublicByQuery(String query, String page, String countFlag) {
        SumPagedRep<Product> pagedRep = productDetailManager.readByQuery(RestfulEntityManager.RoleEnum.PUBLIC, query, page, countFlag, Product.class);
        return new PublicProductSumPagedRep(pagedRep);
    }

    @Transactional(readOnly = true)
    public AppProductSumPagedRep readForAppByQuery(String query, String page, String countFlag) {
        SumPagedRep<Product> pagedRep = productDetailManager.readByQuery(RestfulEntityManager.RoleEnum.APP, query, page, countFlag, Product.class);
        return new AppProductSumPagedRep(pagedRep);
    }

    @Transactional(readOnly = true)
    public PublicProductRep readForPublicById(Long id) {
        SumPagedRep<Product> productDetailSumPagedRep = productDetailManager.readById(RestfulEntityManager.RoleEnum.PUBLIC, id.toString(), Product.class);
        if (productDetailSumPagedRep.getData().size() == 0)
            throw new ProductNotFoundException();
        return new PublicProductRep(productDetailSumPagedRep.getData().get(0), attributeApplicationService);
    }

    @Transactional(readOnly = true)
    public AdminProductRep readForAdminById(Long id) {
        Product productDetail = getForAdminProductDetail(id);
        return new AdminProductRep(productDetail);
    }

    @Transactional
    public AdminProductCreatedRep createForAdmin(AdminCreateProductCommand command) {
        return new AdminProductCreatedRep(Product.create(idGenerator.getId(), command, repo));
    }

    @Transactional
    public void replaceForAdminById(Long id, AdminUpdateProductCommand command) {
        Product productDetail = getForAdminProductDetail(id);
        productDetail.replace(command, this, repo);
    }

    @Transactional
    public Integer deleteForAdminById(Long id) {
        productSkuManager.deleteById(RestfulEntityManager.RoleEnum.ADMIN, id.toString(), ProductSku.class);
        return productDetailManager.deleteById(RestfulEntityManager.RoleEnum.ADMIN, id.toString(), Product.class);
    }

    @Transactional
    public AdminProductRep patchForAdminById(Long id, JsonPatch patch) {
        Product productDetail = getForAdminProductDetail(id);
        return new AdminProductRep(ProductPatchMiddleLayer.doPatch(patch, productDetail, om, repo));
    }

    @Transactional
    public AdminProductSumPagedRep patchForAdmin(List<PatchCommand> commands, String changeId) {
        if (changeHistoryRepository.findByChangeId(changeId + REVOKE).isPresent()) {
            throw new HangingTransactionException();
        }
        saveChangeRecord(commands, changeId);
        List<PatchCommand> deepCopy = getDeepCopy(commands);
        List<PatchCommand> hasNestedEntity = deepCopy.stream().filter(e -> e.getPath().contains("/" + ADMIN_REP_SKU_LITERAL)).collect(Collectors.toList());
        List<PatchCommand> noNestedEntity = deepCopy.stream().filter(e -> !e.getPath().contains("/" + ADMIN_REP_SKU_LITERAL)).collect(Collectors.toList());
        Integer update1 = productSkuManager.update(RestfulEntityManager.RoleEnum.ADMIN, hasNestedEntity, ProductSku.class);
        Integer update = productDetailManager.update(RestfulEntityManager.RoleEnum.ADMIN, noNestedEntity, Product.class);
        return new AdminProductSumPagedRep(update.longValue());
    }

    @Transactional
    public AppProductSumPagedRep patchForApp(List<PatchCommand> commands, String changeId) {
        if (changeHistoryRepository.findByChangeId(changeId + REVOKE).isPresent()) {
            throw new HangingTransactionException();
        }
        saveChangeRecord(commands, changeId);
        List<PatchCommand> deepCopy = getDeepCopy(commands);
        List<PatchCommand> hasNestedEntity = deepCopy.stream().filter(e -> e.getPath().contains("/" + ADMIN_REP_SKU_LITERAL)).collect(Collectors.toList());
        List<PatchCommand> noNestedEntity = deepCopy.stream().filter(e -> !e.getPath().contains("/" + ADMIN_REP_SKU_LITERAL)).collect(Collectors.toList());
        Integer update = productDetailManager.update(RestfulEntityManager.RoleEnum.APP, noNestedEntity, Product.class);
        Integer update1 = productSkuManager.update(RestfulEntityManager.RoleEnum.APP, hasNestedEntity, ProductSku.class);
        return new AppProductSumPagedRep(update.longValue());
    }


    @Transactional
    public Integer deleteForAdminByQuery(String query) {
        //delete sku first
        productSkuManager.deleteByQuery(RestfulEntityManager.RoleEnum.ADMIN, query, ProductSku.class);
        return productDetailManager.deleteByQuery(RestfulEntityManager.RoleEnum.ADMIN, query, Product.class);
    }

    @Transactional
    public void rollbackChangeForApp(String id) {
        log.info("start of rollback change {}", id);
        if (changeHistoryRepository.findByChangeId(id + REVOKE).isPresent()) {
            throw new HangingTransactionException();
        }
        Optional<ChangeRecord> byChangeId = changeHistoryRepository.findByChangeId(id);
        if (byChangeId.isPresent()) {
            ChangeRecord changeRecord = byChangeId.get();
            List<PatchCommand> rollbackCmd = buildRollbackCommand(changeRecord.getPatchCommands());
            patchForApp(rollbackCmd, id + REVOKE);
        }
    }

    private List<PatchCommand> buildRollbackCommand(List<PatchCommand> patchCommands) {
        List<PatchCommand> deepCopy = getDeepCopy(patchCommands);
        deepCopy.forEach(e -> {
            if (e.getOp().equalsIgnoreCase(PATCH_OP_TYPE_SUM)) {
                e.setOp(PATCH_OP_TYPE_DIFF);
            } else if (e.getOp().equalsIgnoreCase(PATCH_OP_TYPE_DIFF)) {
                e.setOp(PATCH_OP_TYPE_SUM);
            } else {
                throw new RollbackNotSupportedException();
            }
        });
        return deepCopy;
    }

    private List<PatchCommand> getDeepCopy(List<PatchCommand> patchCommands) {
        List<PatchCommand> deepCopy;
        try {
            deepCopy = om.readValue(om.writeValueAsString(patchCommands), new TypeReference<List<PatchCommand>>() {
            });
        } catch (IOException e) {
            log.error("error during deep copy", e);
            throw new DeepCopyException();
        }
        return deepCopy;
    }


    private Product getForAdminProductDetail(Long id) {
        SumPagedRep<Product> productDetailSumPagedRep = productDetailManager.readById(RestfulEntityManager.RoleEnum.ADMIN, id.toString(), Product.class);
        if (productDetailSumPagedRep.getData().size() == 0)
            throw new ProductNotFoundException();
        return productDetailSumPagedRep.getData().get(0);
    }

    private void saveChangeRecord(List<PatchCommand> details, String changeId) {
        ChangeRecord changeRecord = new ChangeRecord();
        changeRecord.setPatchCommands((ArrayList<PatchCommand>) details);
        changeRecord.setChangeId(changeId);
        changeRecord.setId(idGenerator.getId());
        changeHistoryRepository.save(changeRecord);
    }

}

