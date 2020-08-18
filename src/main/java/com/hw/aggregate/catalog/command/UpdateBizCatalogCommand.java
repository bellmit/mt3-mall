package com.hw.aggregate.catalog.command;

import com.hw.aggregate.catalog.model.BizCatalog;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateBizCatalogCommand {
    private String name;
    private Long parentId;
    private Set<String> attributes;
    private BizCatalog.CatalogType catalogType;
}