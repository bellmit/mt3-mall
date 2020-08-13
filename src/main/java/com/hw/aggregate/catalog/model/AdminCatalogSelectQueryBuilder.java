package com.hw.aggregate.catalog.model;

import com.hw.shared.sql.builder.SelectQueryBuilder;
import com.hw.shared.sql.clause.SelectFieldEnumStringEqualClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

import static com.hw.aggregate.catalog.model.Catalog.NAME_LITERAL;
import static com.hw.aggregate.catalog.model.Catalog.TYPE_LITERAL;


@Component
public class AdminCatalogSelectQueryBuilder extends SelectQueryBuilder<Catalog> {
    @Autowired
    private void setEntityManager(EntityManager entityManager) {
        em = entityManager;
    }

    AdminCatalogSelectQueryBuilder() {
        DEFAULT_PAGE_SIZE = 1000;
        MAX_PAGE_SIZE = 2000;
        mappedSortBy.put("name", NAME_LITERAL);
        supportedWhereField.put("type", new SelectFieldEnumStringEqualClause<>(TYPE_LITERAL));
        allowEmptyClause=true;
    }
}
