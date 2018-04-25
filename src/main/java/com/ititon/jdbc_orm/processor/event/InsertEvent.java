package com.ititon.jdbc_orm.processor.event;

import java.sql.Connection;

public class InsertEvent {

    private Connection connection;
    private Object entity;

    public InsertEvent(Connection connection, Object entity) {
        this.connection = connection;
        this.entity = entity;
    }

    public Connection getConnection() {
        return connection;
    }

    public Object getEntity() {
        return entity;
    }
}
