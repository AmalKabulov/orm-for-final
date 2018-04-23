package com.ititon.jdbc_orm.processor.query_builder;

import com.ititon.jdbc_orm.annotation.Column;
import com.ititon.jdbc_orm.annotation.Id;
import com.ititon.jdbc_orm.annotation.Table;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.Assert;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class GenericQuery {

    private static final CacheProcessor CACHE_PROCESSOR = CacheProcessor.getInstance();

    public static String buildCountQuery(final Class<? /*extends BaseEntity*/> clazz) throws DefaultOrmException {

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

    public static String buildFindAllQuery(final Class<? /*extends BaseEntity*/> clazz) throws DefaultOrmException {
        return SelectQuery.buildFindAllQuery(clazz);

    }


    public static String findByLimit(final Class<? /*extends BaseEntity*/> clazz, final int skip, final int count) throws DefaultOrmException {
        String findAllQuery = buildFindAllQuery(clazz);
        StringBuilder limitQuery = new StringBuilder(findAllQuery);
        limitQuery.setLength(limitQuery.length() - 1);

        return limitQuery.append(" limit ").append(skip).append(", ").append(count).append(";").toString();
    }


    public static String findByIdQuery(final Class<? /*extends BaseEntity*/> clazz, Object id) throws DefaultOrmException {

        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(clazz);
        Assert.notNull(entityMeta, "entity " + clazz + " not found");

        String findAllQuery = buildFindAllQuery(clazz);
        StringBuilder byIdQuery = new StringBuilder(findAllQuery);
        byIdQuery.setLength(byIdQuery.length() - 1);


        return byIdQuery.append(" where ")
                .append(entityMeta.getTableName()).append(".").append(entityMeta.getIdColumnName())
                .append(" = ")
                .append(id).append(";").toString();

    }

    public static String insertQuery(final Object entity) throws DefaultOrmException {


        InsertQuery.buildInsertQuery(entity).forEach((query) -> System.out.println("INSERT QUERY: " + query));

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

    public static String updateQuery(final Object entity) throws DefaultOrmException {

        Class<?> entityClass = entity.getClass();
        Map<String, String> columnsValues = getColumnsValues(entity);
        String tableName = entityClass.getAnnotation(Table.class).name();
        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(entityClass);
        Assert.notNull(entityMeta, "Entity: " + entityClass + " not found");

        String idColumnFieldName = entityMeta.getIdColumnFieldName();
        String idColumnName = entityMeta.getIdColumnName();
        Object id = ReflectionUtil.invokeGetter(entity, idColumnFieldName);

        StringBuilder query = new StringBuilder("update ").append(tableName).append(" set ");
        List<String> updateValues = columnsValues.entrySet()
                .stream()
                .map(cv -> cv.getKey() + " = " + cv.getValue())
                .collect(Collectors.toList());

        String values = String.join(", ", updateValues);


        return query.append(values).append(" where ").append(idColumnName).append(" = ").append(id).append(";").toString();

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


    private static String getColumns(EntityMeta entityMeta) {
        Collection<FieldMeta> values = entityMeta.getFieldMetas().values();
        List<String> allColumns = values.stream().map(FieldMeta::getColumnName).collect(Collectors.toList());
        return String.join(", ", allColumns);
    }


    private static String wrap(String value) {
        return "\'" + value + "\'";
    }


}
