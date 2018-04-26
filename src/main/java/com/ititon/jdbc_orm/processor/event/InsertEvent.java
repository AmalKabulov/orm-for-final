package com.ititon.jdbc_orm.processor.event;

import java.sql.Connection;
import java.sql.SQLException;

public class InsertEvent {

    private Connection connection;
    private Object entity;

    public InsertEvent(Connection connection, Object entity) throws SQLException {
        connection.setAutoCommit(false);
        this.connection = connection;
        this.entity = entity;
    }

    //TODO удалить этот конструктор
    public InsertEvent(Object entity) {
        this.entity = entity;
    }

    public Connection getConnection() {
        return connection;
    }

    public Object getEntity() {
        return entity;
    }
}
