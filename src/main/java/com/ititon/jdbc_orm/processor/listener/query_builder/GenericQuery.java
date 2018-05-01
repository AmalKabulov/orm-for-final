package com.ititon.jdbc_orm.processor.listener.query_builder;

import com.ititon.jdbc_orm.annotation.Column;
import com.ititon.jdbc_orm.annotation.Id;
import com.ititon.jdbc_orm.annotation.Table;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.Assert;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class GenericQuery {

    private static final CacheProcessor CACHE_PROCESSOR = CacheProcessor.getInstance();

    public static String buildCountQuery(final Class<?> clazz) throws DefaultOrmException {

        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(clazz);
        Assert.notNull(entityMeta, "entity " + clazz + " not found");
        String idColumnName = entityMeta.getIdColumnName();
        String tableName = entityMeta.getTableName();

        return "select count(" +
                tableName + "." + idColumnName +
                ") from " +
                tableName +
                ";";
    }

    public static String buildSelectQuery(final Class<? /*extends BaseEntity*/> clazz) throws DefaultOrmException {
        return SelectQuery.buildSelectQuery(clazz);

    }


    public static String findByLimit(final Class<? /*extends BaseEntity*/> clazz, final int skip, final int count) throws DefaultOrmException {
        String findAllQuery = buildSelectQuery(clazz);
        StringBuilder limitQuery = new StringBuilder(findAllQuery);
        limitQuery.setLength(limitQuery.length() - 1);

        return limitQuery.append(" limit ").append(skip).append(", ").append(count).append(";").toString();
    }


    public static String findByIdQuery(final Class<? /*extends BaseEntity*/> clazz, Object id) throws DefaultOrmException {

        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(clazz);
        Assert.notNull(entityMeta, "entity " + clazz + " not found");

        String selectQuery = buildSelectQuery(clazz);
        StringBuilder byIdQuery = new StringBuilder(selectQuery);
        byIdQuery.setLength(byIdQuery.length() - 1);


        return byIdQuery.append(" where ")
                .append(entityMeta.getTableName()).append(".").append(entityMeta.getIdColumnName())
                .append(" = ")
                .append(id).append(";").toString();

    }

    public static String insertQuery(final Object entity) throws DefaultOrmException {
        Class<?> entityClass = entity.getClass();
        Map<String, String> columnsValues = getColumnsValues(entity);

        String tableName = entityClass.getAnnotation(Table.class).name();
        String columns = String.join(", ", columnsValues.keySet());
        String values = String.join(", ", columnsValues.values());

        return "insert into " +
                tableName +
                " (" + columns +
                ") values (" +
                values + ")" +
                ";";

    }


    public static String deleteQuery(final Class<? /*extends BaseEntity*/> clazz, final Serializable id) throws DefaultOrmException {
        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(clazz);
        Assert.notNull(entityMeta, "Entity: " + clazz + " not found");
        String tableName = entityMeta.getTableName();
        String idColumnName = entityMeta.getIdColumnName();

        return "delete from " +
                tableName + " where " +
                idColumnName +
                " = " + id;
    }


    private static Map<String, String> getColumnsValues(Object entity) {

        Class<?> entityClass = entity.getClass();
        Map<String, String> columnsValues = new LinkedHashMap<>();
        Field[] declaredFields = entityClass.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Column.class)) {
                String columnName = field.getAnnotation(Column.class).name();
                if (!field.isAnnotationPresent(Id.class)) {
                    String columnValue = wrap(String.valueOf(ReflectionUtil.invokeGetter(entity, field.getName())));
                    columnsValues.put(columnName, columnValue);
                }
            }

        }

        return columnsValues;
    }




    private static String wrap(String value) {
        return "\'" + value + "\'";
    }


}
