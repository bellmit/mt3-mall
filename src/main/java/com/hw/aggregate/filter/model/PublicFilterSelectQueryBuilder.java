package com.hw.aggregate.filter.model;

import com.hw.shared.sql.builder.SelectQueryBuilder;
import com.hw.shared.sql.clause.SelectFieldStringEqualClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

import static com.hw.aggregate.filter.model.BizFilter.ENTITY_CATALOG_LITERAL;

@Component
public class PublicFilterSelectQueryBuilder extends SelectQueryBuilder<BizFilter> {

    PublicFilterSelectQueryBuilder() {
        DEFAULT_PAGE_SIZE = 1;
        MAX_PAGE_SIZE = 5;
        supportedWhereField.put("catalog", new SelectFieldStringEqualClause<>(ENTITY_CATALOG_LITERAL));
    }
}
