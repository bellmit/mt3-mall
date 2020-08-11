package com.hw.aggregate.filter.model;

import com.hw.shared.SelectQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.hw.aggregate.filter.model.BizFilter.ID_LITERAL;
import static com.hw.aggregate.filter.model.BizFilter.LINKED_CATALOG_LITERAL;

@Component
public class AdminFilterSelectQueryBuilder extends SelectQueryBuilder<BizFilter> {
    @Autowired
    private void setEntityManager(EntityManager entityManager) {
        em = entityManager;
    }

    AdminFilterSelectQueryBuilder() {
        DEFAULT_PAGE_SIZE = 40;
        MAX_PAGE_SIZE = 400;
    }

    public Predicate getWhereClause(Root<BizFilter> root, String search) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        if (search == null)
            return null;
        String[] queryParams = search.split(",");
        List<Predicate> results = new ArrayList<>();
        for (String param : queryParams) {
            String[] split = param.split(":");
            if (split.length == 2) {
                if ("catalog".equals(split[0]) && !split[1].isBlank()) {
                    results.add(cb.like(root.get(LINKED_CATALOG_LITERAL).as(String.class), "%" + split[1] + "%"));
                }
            }
        }
        return cb.and(results.toArray(new Predicate[0]));
    }
}