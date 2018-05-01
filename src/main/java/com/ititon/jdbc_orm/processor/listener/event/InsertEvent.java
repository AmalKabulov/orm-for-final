package com.ititon.jdbc_orm.processor.listener.event;

import java.sql.Connection;
import java.sql.SQLException;

public class InsertEvent extends Event {

    public enum Type {
        UPDATE, SAVE
    }

    private Type type;
//    private Connection connection;
    private Object entity;

    public InsertEvent(Connection connection, Type type, Object entity) throws SQLException {
        super(connection);
        connection.setAutoCommit(false);
        this.type = type;
//        this.connection = connection;
        this.entity = entity;
    }


//    public Connection getConnection() {
//        return connection;
//    }

    public Object getEntity() {
        return entity;
    }

    public Type getType() {
        return type;
    }
}
