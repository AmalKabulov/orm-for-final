package com.ititon.jdbc_orm.meta;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityMeta {

    private String entityClassName;
    private String tableName;

    private String idColumnName;
    private String idColumnFieldName;
    private Class<?> idColumnType;

//    List<FieldMeta> fieldMetas = new ArrayList<>();

    private Map<String, FieldMeta> fieldMetas = new LinkedHashMap<>();

    public EntityMeta() {
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public void setEntityClassName(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    public void setIdColumnName(String idColumnName) {
        this.idColumnName = idColumnName;
    }


    public Map<String, FieldMeta> getFieldMetas() {
        return fieldMetas;
    }

    public void setFieldMetas(Map<String, FieldMeta> fieldMetas) {
        this.fieldMetas = fieldMetas;
    }

    public String getIdColumnFieldName() {
        return idColumnFieldName;
    }

    public void setIdColumnFieldName(String idColumnFieldName) {
        this.idColumnFieldName = idColumnFieldName;
    }

    public Class<?> getIdColumnType() {
        return idColumnType;
    }

    public void setIdColumnType(Class<?> idColumnType) {
        this.idColumnType = idColumnType;
    }
}
