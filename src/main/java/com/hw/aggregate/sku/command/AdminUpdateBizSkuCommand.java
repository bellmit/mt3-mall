package com.hw.aggregate.sku.command;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class AdminUpdateBizSkuCommand implements Serializable {
    private static final long serialVersionUID = 1;
    private BigDecimal price;
    private String description;
}