package com.hw.aggregate.product.model;

import com.hw.aggregate.product.ProductApplicationService;
import com.hw.aggregate.product.ProductDetailRepo;
import com.hw.aggregate.product.command.*;
import com.hw.aggregate.product.exception.*;
import com.hw.shared.Auditable;
import com.hw.shared.StringSetConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hw.config.AppConstant.ADMIN_ADJUST;


@Data
@Entity
@Table
@NoArgsConstructor
@Slf4j
//@TODO lowestPrice can only present if sku is empty
//@TODO if sku is empty then storageOrder, storageActual, lowestPrice, totalSales must present
//@TODO if storageOrder, storageActual, lowestPrice, totalSales are empty then sku must present
public class ProductDetail extends Auditable {
    @Id
    private Long id;
    public transient static final String ID_LITERAL = "id";

    private String imageUrlSmall;

    private String name;
    public transient static final String NAME_LITERAL = "name";

    private String description;

    private Long endAt;
    public transient static final String END_AT_LITERAL = "endAt";

    private Long startAt;
    public transient static final String START_AT_LITERAL = "startAt";

    @Column(length = 10000)
    @Convert(converter = ProductOptionConverter.class)
    private List<ProductOption> selectedOptions;
    public transient static final String SELECTED_OPTIONS_LITERAL = "selectedOptions";

    @Convert(converter = StringSetConverter.class)
    private Set<String> imageUrlLarge;
    public transient static final String IMAGE_URL_LARGE_LITERAL = "imageUrlLarge";

    @Convert(converter = StringSetConverter.class)
    private Set<String> specification;
    public transient static final String SPEC_LITERAL = "specification";

    @Convert(converter = StringSetConverter.class)
    private Set<String> attrKey;
    public transient static final String ATTR_KEY_LITERAL = "attrKey";

    @Convert(converter = StringSetConverter.class)
    private Set<String> attrProd;
    public transient static final String ATTR_PROD_LITERAL = "attrProd";

    @Convert(converter = StringSetConverter.class)
    private Set<String> attrGen;
    public transient static final String ATTR_GEN_LITERAL = "attrGen";

    @Convert(converter = StringSetConverter.class)
    private Set<String> attrSalesTotal;
    public transient static final String ATTR_SALES_TOTAL_LITERAL = "attrSalesTotal";

    @ElementCollection
    @CollectionTable(name = "product_sku_map", joinColumns = @JoinColumn(name = "product_id"), uniqueConstraints = @UniqueConstraint(columnNames = {"attributesSales", "product_id"}))
    private List<ProductSku> productSkuList;

    @Column(length = 10000)
    private ArrayList<ProductAttrSaleImages> attributeSaleImages;

    @Column(updatable = false)
    private Integer storageOrder;
    public transient static final String STORAGE_ORDER_LITERAL = "storageOrder";

    @Column(updatable = false)
    private Integer storageActual;
    public transient static final String STORAGE_ACTUAL_LITERAL = "storageActual";

    private BigDecimal lowestPrice;
    public transient static final String LOWEST_PRICE_LITERAL = "lowestPrice";

    @Column(updatable = false)
    private Integer totalSales;
    public transient static final String TOTAL_SALES_LITERAL = "totalSales";

    public static ProductDetail create(Long id, CreateProductAdminCommand command, ProductDetailRepo repo) {
        ProductDetail productDetail = new ProductDetail(id, command);
        return repo.save(productDetail);
    }

    public static ProductDetail readAdmin(Long id, ProductDetailRepo repo) {
        Optional<ProductDetail> findById = repo.findById(id);
        if (findById.isEmpty())
            throw new ProductNotFoundException();
        return findById.get();
    }

    public static ProductDetail readCustomer(Long id, ProductDetailRepo repo) {
        Optional<ProductDetail> findById = repo.findById(id);
        if (findById.isEmpty())
            throw new ProductNotFoundException();
        if (!ProductDetail.isAvailable(findById.get()))
            throw new ProductNotAvailableException();
        return findById.get();
    }

    public static boolean isAvailable(ProductDetail productDetail) {
        Long current = new Date().getTime();
        if (productDetail.getStartAt() == null)
            return false;
        if (current.compareTo(productDetail.getStartAt()) < 0) {
            return false;
        } else {
            if (productDetail.getEndAt() == null) {
                return true;
            } else if (current.compareTo(productDetail.getEndAt()) < 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean validate(List<ProductValidationCommand> commands, ProductDetailRepo repo) {
        return commands.stream().anyMatch(command -> {
            Optional<ProductDetail> byId = repo.findById(Long.parseLong(command.getProductId()));
            //validate product match
            if (byId.isEmpty() || !ProductDetail.isAvailable(byId.get()))
                return true;
            BigDecimal price;
            if (byId.get().getProductSkuList() != null && byId.get().getProductSkuList().size() != 0) {
                List<ProductSku> collect = byId.get().getProductSkuList().stream().filter(productSku -> new TreeSet(productSku.getAttributesSales()).equals(new TreeSet(command.getAttributesSales()))).collect(Collectors.toList());
                price = collect.get(0).getPrice();
            } else {
                price = byId.get().getLowestPrice();
            }
            //if no option present then compare final price
            if (command.getSelectedOptions() == null || command.getSelectedOptions().size() == 0) {
                return price.compareTo(command.getFinalPrice()) != 0;
            }
            //validate product option match
            List<ProductOption> storedOption = byId.get().getSelectedOptions();
            if (storedOption == null || storedOption.size() == 0)
                return true;
            boolean optionAllMatch = command.getSelectedOptions().stream().allMatch(userSelected -> {
                //check selected option is valid option
                Optional<ProductOption> first = storedOption.stream().filter(storedOptionItem -> {
                    // compare title
                    if (!storedOptionItem.title.equals(userSelected.title))
                        return false;
                    //compare option value for each title
                    String optionValue = userSelected.getOptions().get(0).getOptionValue();
                    Optional<OptionItem> first1 = storedOptionItem.options.stream().filter(optionItem -> optionItem.getOptionValue().equals(optionValue)).findFirst();
                    if (first1.isEmpty())
                        return false;
                    return true;
                }).findFirst();
                if (first.isEmpty())
                    return false;
                else {
                    return true;
                }
            });
            if (!optionAllMatch)
                return true;
            //validate product final price
            BigDecimal finalPrice = command.getFinalPrice();
            // get all price variable
            List<String> userSelectedAddOnTitles = command.getSelectedOptions().stream().map(ProductOption::getTitle).collect(Collectors.toList());
            // filter option based on title
            Stream<ProductOption> storedAddonMatchingUserSelection = byId.get().getSelectedOptions().stream().filter(var1 -> userSelectedAddOnTitles.contains(var1.getTitle()));
            // map to value detail for each title
            List<String> priceVarCollection = storedAddonMatchingUserSelection.map(storedMatchAddon -> {
                String title = storedMatchAddon.getTitle();
                //find right option for title
                Optional<ProductOption> user_addon_option = command.getSelectedOptions().stream().filter(e -> e.getTitle().equals(title)).findFirst();
                OptionItem user_optionItem = user_addon_option.get().getOptions().get(0);
                Optional<OptionItem> first = storedMatchAddon.getOptions().stream().filter(db_optionItem -> db_optionItem.getOptionValue().equals(user_optionItem.getOptionValue())).findFirst();
                return first.get().getPriceVar();
            }).collect(Collectors.toList());
            BigDecimal calc = new BigDecimal(0);
            for (String priceVar : priceVarCollection) {
                if (priceVar.contains("+")) {
                    double v = Double.parseDouble(priceVar.replace("+", ""));
                    BigDecimal bigDecimal = BigDecimal.valueOf(v);
                    calc = calc.add(bigDecimal);
                } else if (priceVar.contains("-")) {
                    double v = Double.parseDouble(priceVar.replace("-", ""));
                    BigDecimal bigDecimal = BigDecimal.valueOf(v);
                    calc = calc.subtract(bigDecimal);

                } else if (priceVar.contains("*")) {
                    double v = Double.parseDouble(priceVar.replace("*", ""));
                    BigDecimal bigDecimal = BigDecimal.valueOf(v);
                    calc = calc.multiply(bigDecimal);
                } else {
                    log.error("unknown operation type");
                }
            }
            if (calc.add(price).compareTo(finalPrice) == 0) {
                log.error("value does match for product {}, expected {} actual {}", command.getProductId(), calc.add(price), finalPrice);
                return false;
            }
            return true;
        });
    }

    public static void delete(Long id, ProductDetailRepo repo) {
        ProductDetail read = readAdmin(id, repo);
        repo.delete(read);
    }

    public void update(UpdateProductAdminCommand command, ProductApplicationService productApplicationService, ProductDetailRepo repo) {
        this.imageUrlSmall = command.getImageUrlSmall();
        this.name = command.getName();
        this.description = command.getDescription();
        this.selectedOptions = command.getSelectedOptions();
        this.imageUrlLarge = command.getImageUrlLarge();
        this.specification = command.getSpecification();
        this.attrKey = command.getAttributesKey();
        this.attrProd = command.getAttributesProd();
        this.attrGen = command.getAttributesGen();
        this.startAt = command.getStartAt();
        this.endAt = command.getEndAt();
        if (command.getSkus() != null && command.getSkus().size() != 0) {
            command.getSkus().forEach(e -> {
                if (e.getSales() == null)
                    e.setSales(0);
                e.setAttributesSales(new TreeSet<>(e.getAttributesSales()));
            });
            adjustSku(command.getSkus(), productApplicationService);
            this.attrSalesTotal = command.getSkus().stream().map(UpdateProductAdminCommand.UpdateProductAdminSkuCommand::getAttributesSales).flatMap(Collection::stream).collect(Collectors.toSet());
            this.attributeSaleImages = command.getAttributeSaleImages().stream().map(e ->
                    {
                        ProductAttrSaleImages productAttrSaleImages = new ProductAttrSaleImages();
                        productAttrSaleImages.setAttributeSales(e.getAttributeSales());
                        productAttrSaleImages.setImageUrls(e.getImageUrls());
                        return productAttrSaleImages;
                    }
            ).collect(Collectors.toCollection(ArrayList::new));
            this.lowestPrice = findLowestPrice(this);
        } else {
            this.productSkuList = null;
            this.lowestPrice = command.getPrice();
            if (command.getDecreaseOrderStorage() != null) {
                DecreaseOrderStorageCommand command1 = new DecreaseOrderStorageCommand();
                command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
                command1.setChangeList(getStorageChangeDetail(command.getDecreaseOrderStorage()));
                productApplicationService.decreaseOrderStorageForMappedProducts(command1);
            }
            if (command.getDecreaseActualStorage() != null) {
                DecreaseActualStorageCommand command1 = new DecreaseActualStorageCommand();
                command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
                command1.setChangeList(getStorageChangeDetail(command.getDecreaseActualStorage()));
                productApplicationService.decreaseActualStorageForMappedProductsAdmin(command1);
            }
            if (command.getIncreaseOrderStorage() != null) {
                IncreaseOrderStorageCommand command1 = new IncreaseOrderStorageCommand();
                command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
                command1.setChangeList(getStorageChangeDetail(command.getIncreaseOrderStorage()));
                productApplicationService.increaseOrderStorageForMappedProducts(command1);
            }
            if (command.getIncreaseActualStorage() != null) {
                IncreaseActualStorageCommand command1 = new IncreaseActualStorageCommand();
                command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
                command1.setChangeList(getStorageChangeDetail(command.getIncreaseActualStorage()));
                productApplicationService.increaseActualStorageForMappedProductsAdmin(command1);
            }
        }
        repo.save(this);
    }

    public void updateStatus(ProductStatus status, ProductDetailRepo repo) {
        Long current = new Date().getTime();
        if (ProductStatus.AVAILABLE.equals(status)) {
            //make product available
            if (this.startAt != null && this.endAt != null) {
                if (this.startAt.compareTo(current) <= 0 && this.endAt.compareTo(current) > 0) {
                    //do nothing, product is already available
                } else {
                    if (this.startAt.compareTo(current) > 0) {
                        this.startAt = new Date().getTime();
                    } else {
                        //this.endAt.compareTo(current) <= 0
                        //set endAt to null, user need to manual update endAt
                        this.endAt = null;
                    }
                }
            } else if (this.startAt != null && this.endAt == null) {
                if (this.startAt.compareTo(current) >= 0) {
                    this.startAt = current;
                } else {
                    //do nothing
                }
            } else if (this.startAt == null && this.endAt == null) {
                this.startAt = current;
            } else if (this.startAt == null && this.endAt != null) {
                this.startAt = current;
                if (this.endAt.compareTo(current) > 0) {
                    //do nothing
                } else {
                    this.endAt = null;
                }
            }
        } else {
            //make product unavailable
            if (this.startAt != null && this.endAt != null) {
                if (this.startAt.compareTo(current) <= 0 && this.endAt.compareTo(current) > 0) {
                    this.endAt = current;
                } else {
                    if (this.startAt.compareTo(current) > 0) {
                        this.startAt = null;
                    } else {
                        //do nothing
                    }
                }
            } else if (this.startAt != null && this.endAt == null) {
                this.startAt = null;
            } else if (this.startAt == null && this.endAt == null) {
                //do nothing
            } else if (this.startAt == null && this.endAt != null) {
                //do nothing
            }
        }
        repo.save(this);
    }

    private void adjustSku(List<UpdateProductAdminCommand.UpdateProductAdminSkuCommand> commands, ProductApplicationService productApplicationService) {
        commands.forEach(command -> {
            if (command.getStorageActual() != null && command.getStorageOrder() != null) {
                // new sku
                boolean b = this.productSkuList.stream().anyMatch(e -> e.getAttributesSales().equals(command.getAttributesSales()));
                if (b)
                    throw new SkuAlreadyExistException();
                ProductSku productSku = new ProductSku();
                productSku.setSales(command.getSales() == null ? 0 : command.getSales());
                productSku.setStorageActual(command.getStorageActual());
                productSku.setStorageOrder(command.getStorageOrder());
                productSku.setAttributesSales(command.getAttributesSales());
                productSku.setPrice(command.getPrice());
                this.productSkuList.add(productSku);
            } else {
                //existing sku
                Optional<ProductSku> first = this.productSkuList.stream().filter(e -> e.getAttributesSales().equals(command.getAttributesSales())).findFirst();
                if (first.isEmpty())
                    throw new SkuNotExistException();
                //update price
                ProductSku productSku = first.get();
                productSku.setPrice(command.getPrice());
                updateStorage(productApplicationService, command);

            }
        });
        // find skus not in update command & remove
        List<ProductSku> collect = this.productSkuList.stream().filter(e -> commands.stream().noneMatch(command -> command.getAttributesSales().equals(e.getAttributesSales()))).collect(Collectors.toList());
        this.productSkuList.removeAll(collect);
    }

    private void updateStorage(ProductApplicationService productApplicationService, UpdateProductAdminCommand.UpdateProductAdminSkuCommand command) {
        if (command.getDecreaseOrderStorage() != null) {
            DecreaseOrderStorageCommand command1 = new DecreaseOrderStorageCommand();
            command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
            command1.setChangeList(getStorageChangeDetail(command, command.getDecreaseOrderStorage()));
            productApplicationService.decreaseOrderStorageForMappedProducts(command1);
        }
        if (command.getDecreaseActualStorage() != null) {
            DecreaseActualStorageCommand command1 = new DecreaseActualStorageCommand();
            command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
            command1.setChangeList(getStorageChangeDetail(command, command.getDecreaseActualStorage()));
            productApplicationService.decreaseActualStorageForMappedProductsAdmin(command1);
        }
        if (command.getIncreaseOrderStorage() != null) {
            IncreaseOrderStorageCommand command1 = new IncreaseOrderStorageCommand();
            command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
            command1.setChangeList(getStorageChangeDetail(command, command.getIncreaseOrderStorage()));
            productApplicationService.increaseOrderStorageForMappedProducts(command1);
        }
        if (command.getIncreaseActualStorage() != null) {
            IncreaseActualStorageCommand command1 = new IncreaseActualStorageCommand();
            command1.setTxId(UUID.randomUUID().toString() + ADMIN_ADJUST);
            command1.setChangeList(getStorageChangeDetail(command, command.getIncreaseActualStorage()));
            productApplicationService.increaseActualStorageForMappedProductsAdmin(command1);
        }
    }

    private List<StorageChangeDetail> getStorageChangeDetail(UpdateProductAdminCommand.UpdateProductAdminSkuCommand command, Integer increaseOrderStorage) {
        ArrayList<StorageChangeDetail> objects = new ArrayList<>(1);
        StorageChangeDetail storageChangeDetail = new StorageChangeDetail();
        storageChangeDetail.setAmount(increaseOrderStorage);
        storageChangeDetail.setProductId(this.id);
        storageChangeDetail.setAttributeSales(command.getAttributesSales());
        objects.add(storageChangeDetail);
        return objects;
    }

    private List<StorageChangeDetail> getStorageChangeDetail(Integer increaseOrderStorage) {
        ArrayList<StorageChangeDetail> objects = new ArrayList<>(1);
        StorageChangeDetail storageChangeDetail = new StorageChangeDetail();
        storageChangeDetail.setAmount(increaseOrderStorage);
        storageChangeDetail.setProductId(this.id);
        objects.add(storageChangeDetail);
        return objects;
    }


    private ProductDetail(Long id, CreateProductAdminCommand command) {
        this.id = id;
        this.imageUrlSmall = command.getImageUrlSmall();
        this.name = command.getName();
        this.description = command.getDescription();
        this.selectedOptions = command.getSelectedOptions();
        this.imageUrlLarge = command.getImageUrlLarge();
        this.specification = command.getSpecification();
        this.attrKey = command.getAttributesKey();
        this.attrProd = command.getAttributesProd();
        this.attrGen = command.getAttributesGen();
        this.startAt = (command.getStartAt());
        this.endAt = (command.getEndAt());
        if (command.getSkus() != null && command.getSkus().size() != 0) {
            command.getSkus().forEach(e -> {
                if (e.getSales() == null)
                    e.setSales(0);
                e.setAttributesSales(e.getAttributesSales());
            });
            this.attrSalesTotal = command.getSkus().stream().map(CreateProductAdminCommand.CreateProductSkuAdminCommand::getAttributesSales).flatMap(Collection::stream).collect(Collectors.toSet());
            this.productSkuList = command.getSkus().stream().map(e -> {
                ProductSku productSku = new ProductSku();
                productSku.setPrice(e.getPrice());
                productSku.setAttributesSales(e.getAttributesSales());
                productSku.setStorageOrder(e.getStorageOrder());
                productSku.setStorageActual(e.getStorageActual());
                productSku.setSales(e.getSales());
                return productSku;
            }).collect(Collectors.toList());
            this.attributeSaleImages = command.getAttributeSaleImages().stream().map(e ->
                    {
                        ProductAttrSaleImages productAttrSaleImages = new ProductAttrSaleImages();
                        productAttrSaleImages.setAttributeSales(e.getAttributeSales());
                        productAttrSaleImages.setImageUrls(e.getImageUrls());
                        return productAttrSaleImages;
                    }
            ).collect(Collectors.toCollection(ArrayList::new));
            this.lowestPrice = findLowestPrice(this);
            this.totalSales = calcTotalSales(this);
        } else {
            this.storageOrder = command.getStorageOrder();
            this.storageActual = command.getStorageActual();
            this.totalSales = command.getSales();
            this.lowestPrice = command.getPrice();
        }
    }


    private Integer calcTotalSales(ProductDetail productDetail) {
        return productDetail.getProductSkuList().stream().map(ProductSku::getSales).reduce(0, Integer::sum);
    }

    private BigDecimal findLowestPrice(ProductDetail productDetail) {
        ProductSku productSku = productDetail.getProductSkuList().stream().min(Comparator.comparing(ProductSku::getPrice)).orElseThrow(NoLowestPriceFoundException::new);
        return productSku.getPrice();
    }


}
