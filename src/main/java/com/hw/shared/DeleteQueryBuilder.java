package com.hw.shared;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public abstract class DeleteQueryBuilder<T> implements WhereClause<T> {
    private EntityManager em;
    private CriteriaBuilder cb;

    public Integer delete(String search, Class<T> clazz) {
        CriteriaDelete<T> criteriaDeleteSku = cb.createCriteriaDelete(clazz);
        Root<T> root = criteriaDeleteSku.from(clazz);
        Predicate predicate = getWhereClause(root, search);
        if (predicate != null)
            criteriaDeleteSku.where(predicate);
        return em.createQuery(criteriaDeleteSku).executeUpdate();
    }
}
