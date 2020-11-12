package com.hw.aggregate.sku;

import com.hw.aggregate.sku.command.AdminCreateBizSkuCommand;
import com.hw.aggregate.sku.command.AdminUpdateBizSkuCommand;
import com.hw.aggregate.sku.model.AdminBizSkuPatchMiddleLayer;
import com.hw.aggregate.sku.model.BizSku;
import com.hw.aggregate.sku.representation.AdminBizSkuCardRep;
import com.hw.aggregate.sku.representation.AdminBizSkuRep;
import com.hw.shared.rest.DefaultRoleBasedRestfulService;
import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class AdminBizSkuApplicationService extends DefaultRoleBasedRestfulService<BizSku, AdminBizSkuCardRep, AdminBizSkuRep, AdminBizSkuPatchMiddleLayer> {

    @PostConstruct
    private void setUp() {
        entityClass = BizSku.class;
        role = RestfulQueryRegistry.RoleEnum.ADMIN;
        entityPatchSupplier = (AdminBizSkuPatchMiddleLayer::new);
    }

    @Override
    public BizSku replaceEntity(BizSku bizSku, Object command) {
        return bizSku.replace((AdminUpdateBizSkuCommand)command);
    }

    @Override
    public AdminBizSkuCardRep getEntitySumRepresentation(BizSku bizSku) {
        return new AdminBizSkuCardRep(bizSku);
    }

    @Override
    public AdminBizSkuRep getEntityRepresentation(BizSku bizSku) {
        return new AdminBizSkuRep(bizSku);
    }

    @Override
    protected BizSku createEntity(long id, Object command) {
        return BizSku.create(id,(AdminCreateBizSkuCommand)command);
    }

    @Override
    public void preDelete(BizSku bizSku) {

    }

    @Override
    public void postDelete(BizSku bizSku) {

    }

    @Override
    protected void prePatch(BizSku bizSku, Map<String, Object> params, AdminBizSkuPatchMiddleLayer middleLayer) {

    }

    @Override
    protected void postPatch(BizSku bizSku, Map<String, Object> params, AdminBizSkuPatchMiddleLayer middleLayer) {

    }

}