package com.ititon.jdbc_orm;

import java.util.Objects;

public class ProcessedObject {
    private Class<?> entityClass;
    private String fieldName;


    public ProcessedObject(Class<?> entityClass, String fieldName) {
        this.entityClass = entityClass;
        this.fieldName = fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedObject)) return false;
        ProcessedObject that = (ProcessedObject) o;
        return Objects.equals(entityClass, that.entityClass) &&
                Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(entityClass, fieldName);
    }

}
