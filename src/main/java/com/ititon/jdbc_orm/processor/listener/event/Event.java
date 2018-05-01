package com.ititon.jdbc_orm.processor.listener.event;

import java.sql.Connection;

public abstract class Event {
    private Connection connection;

    public Event(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }
}
