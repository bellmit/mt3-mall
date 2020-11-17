package com.hw.shared.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.shared.Auditable;
import com.hw.shared.cache.CacheCriteria;
import com.hw.shared.sql.builder.SelectQueryBuilder;
import com.hw.shared.sql.builder.SoftDeleteQueryBuilder;
import com.hw.shared.sql.builder.UpdateQueryBuilder;
import com.hw.shared.sql.exception.QueryBuilderNotFoundException;
import com.hw.shared.sql.exception.UnknownRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class RestfulQueryRegistry<T extends Auditable> {
    @Autowired
    ApplicationContext applicationContext;
    ObjectMapper om = new ObjectMapper();
    @Autowired
    StringRedisTemplate redisTemplate;
    protected Map<RoleEnum, SelectQueryBuilder<T>> selectQueryBuilder = new HashMap<>();
    protected Map<RoleEnum, UpdateQueryBuilder<T>> updateQueryBuilder = new HashMap<>();
    protected Map<RoleEnum, SoftDeleteQueryBuilder<T>> deleteQueryBuilder = new HashMap<>();

    public abstract Class<T> getEntityClass();

    @PostConstruct
    protected void configQueryBuilder() {

        String[] beanNamesForType = applicationContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(SelectQueryBuilder.class, getEntityClass()));
        for (String name : beanNamesForType) {
            RoleEnum roleEnum = getRoleEnum(name);
            SelectQueryBuilder<T> builder = (SelectQueryBuilder<T>) applicationContext.getBean(name);
            selectQueryBuilder.put(roleEnum, builder);
        }
        String[] beanNamesForType2 = applicationContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(SoftDeleteQueryBuilder.class, getEntityClass()));
        for (String name : beanNamesForType2) {
            RoleEnum roleEnum = getRoleEnum(name);
            SoftDeleteQueryBuilder<T> builder = (SoftDeleteQueryBuilder<T>) applicationContext.getBean(name);
            deleteQueryBuilder.put(roleEnum, builder);
        }
        String[] beanNamesForType3 = applicationContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(UpdateQueryBuilder.class, getEntityClass()));
        for (String name : beanNamesForType3) {
            RoleEnum roleEnum = getRoleEnum(name);
            UpdateQueryBuilder<T> builder = (UpdateQueryBuilder<T>) applicationContext.getBean(name);
            updateQueryBuilder.put(roleEnum, builder);
        }
    }

    private RoleEnum getRoleEnum(String name) {
        RoleEnum roleEnum;
        if (name.indexOf("app") == 0) {
            roleEnum = RoleEnum.APP;
        } else if (name.indexOf("root") == 0) {
            roleEnum = RoleEnum.ROOT;
        } else if (name.indexOf("user") == 0) {
            roleEnum = RoleEnum.USER;
        } else if (name.indexOf("public") == 0) {
            roleEnum = RoleEnum.PUBLIC;
        } else if (name.indexOf("admin") == 0) {
            roleEnum = RoleEnum.ADMIN;
        } else {
            throw new UnknownRoleException();
        }
        return roleEnum;
    }

    //GET service-name/role-name/entity-collection - read object collection with pagination
    //GET service-name/role-name/object-collection?query={condition-clause}
    public SumPagedRep<T> readByQuery(RoleEnum roleEnum, String query, String page, String config, Class<T> clazz) {
        CacheCriteria cacheCriteria = new CacheCriteria(roleEnum, query, page, config);
        String cache = redisTemplate.opsForValue().get(getEntityClass().getName() + ":" + cacheCriteria.hashCode());
        if (cache == null) {
            SelectQueryBuilder<T> selectQueryBuilder = this.selectQueryBuilder.get(roleEnum);
            if (selectQueryBuilder == null)
                throw new QueryBuilderNotFoundException();
            List<T> select = selectQueryBuilder.select(query, page, clazz);
            Long aLong = null;
            if (!skipCount(config)) {
                aLong = selectQueryBuilder.selectCount(query, clazz);
            }
            SumPagedRep<T> tSumPagedRep = new SumPagedRep<>(select, aLong);
            try {
                String s = om.writeValueAsString(tSumPagedRep);
                redisTemplate.opsForValue().set(getEntityClass().getName() + ":" + cacheCriteria.hashCode(), s);
            } catch (JsonProcessingException e) {
                log.error("error during cache update", e);
            }
            return tSumPagedRep;
        } else {
            try {
                JavaType type = om.getTypeFactory().constructParametricType(SumPagedRep.class, clazz);
                return om.readValue(cache, type);
            } catch (IOException e) {
                log.error("error during read from redis cache", e);
                return new SumPagedRep<T>(Collections.emptyList(), 0L);
            }
        }
    }

    // convert GET service-name/role-name/entity-collection/{entity-id} to ByQuery
    public SumPagedRep<T> readById(RoleEnum roleEnum, String id, Class<T> clazz) {
        return readByQuery(roleEnum, convertIdToQuery(id), null, skipCount(), clazz);
    }

    public Integer deleteByQuery(RoleEnum roleEnum, String query, Class<T> clazz) {
        SoftDeleteQueryBuilder<T> deleteQueryBuilder = this.deleteQueryBuilder.get(roleEnum);
        if (deleteQueryBuilder == null)
            throw new QueryBuilderNotFoundException();
        return deleteQueryBuilder.delete(query, clazz);
    }

    public Integer deleteById(RoleEnum roleEnum, String id, Class<T> clazz) {
        return deleteByQuery(roleEnum, convertIdToQuery(id), clazz);
    }

    public Integer update(RoleEnum roleEnum, List<PatchCommand> commands, Class<T> clazz) {
        UpdateQueryBuilder<T> updateQueryBuilder = this.updateQueryBuilder.get(roleEnum);
        if (updateQueryBuilder == null)
            throw new QueryBuilderNotFoundException();
        return updateQueryBuilder.update(commands, clazz);
    }

    //config=sc:0
    private boolean skipCount(String config) {
        return config != null && config.contains("sc:1");
    }

    private String skipCount() {
        return "sc:1";
    }

    private String convertIdToQuery(String id) {
        return "id:" + id;
    }

    public enum RoleEnum {
        ROOT,
        ADMIN,
        USER,
        APP,
        PUBLIC
    }

}
