package com.ititon.jdbc_orm.meta;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata of entity fields.
 * Used for creating entities meta cache;
 */
public class FieldMeta {
    private String fieldName;
    private String columnName;
    private Class<?> fieldType;
    private Class<?> fieldGenericType;
    private Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();


    public FieldMeta() {
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public void setFieldType(Class<?> fieldType) {
        this.fieldType = fieldType;
    }

    public Class<?> getFieldGenericType() {
        return fieldGenericType;
    }

    public void setFieldGenericType(Class<?> fieldGenericType) {
        this.fieldGenericType = fieldGenericType;
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
        this.annotations = annotations;
    }
}
