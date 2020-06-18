package com.hw.aggregate.product.representation;

import com.hw.aggregate.product.model.ProductDetail;
import com.hw.aggregate.product.model.ProductOption;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class ProductDetailAdminRepresentation {
    private Long id;
    private String imageUrlSmall;
    private String name;
    private Integer orderStorage;
    private Integer increaseOrderStorageBy;
    private Integer decreaseOrderStorageBy;
    private Integer increaseActualStorageBy;
    private Integer decreaseActualStorageBy;
    private String description;
    private String rate;
    private BigDecimal price;
    private Integer sales;
    private String catalog;
    private List<ProductOption> selectedOptions;
    private Set<String> imageUrlLarge;
    private Set<String> specification;

    public ProductDetailAdminRepresentation(ProductDetail productDetail) {
        this.id = productDetail.getId();
        this.imageUrlSmall = productDetail.getImageUrlSmall();
        this.name = productDetail.getName();
        this.orderStorage = productDetail.getOrderStorage();
        this.description = productDetail.getDescription();
        this.rate = productDetail.getRate();
        this.price = productDetail.getPrice();
        this.sales = productDetail.getSales();
        this.catalog = productDetail.getCatalog();
        this.selectedOptions = productDetail.getSelectedOptions();
        this.imageUrlLarge = productDetail.getImageUrlLarge();
        this.specification = productDetail.getSpecification();
    }
}
