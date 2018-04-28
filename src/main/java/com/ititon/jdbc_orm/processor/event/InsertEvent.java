package com.ititon.jdbc_orm.processor.event;

import java.sql.Connection;
import java.sql.SQLException;

public class InsertEvent {

    public enum Type {
        UPDATE, INSERT
    }

    private Type type;
    private Connection connection;
    private Object entity;

    public InsertEvent(Connection connection, Object entity) throws SQLException {
        connection.setAutoCommit(false);
        this.connection = connection;
        this.entity = entity;
        this.type = Type.INSERT;
    }

    public InsertEvent(Type type, Connection connection, Object entity) throws SQLException {
        connection.setAutoCommit(false);
        this.type = type;
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

    public Type getType() {
        return type;
    }
}
