package com.ititon.jdbc_orm.processor.event;

import java.sql.ResultSet;

public class SelectEvent {

    private Class<?> entityClass;
    private ResultSet resultSet;


    public SelectEvent(Class<?> entityClass, ResultSet resultSet) {
        this.entityClass = entityClass;
        this.resultSet = resultSet;
    }


    public Class<?> getEntityClass() {
        return entityClass;
    }



    public ResultSet getResultSet() {
        return resultSet;
    }




}
