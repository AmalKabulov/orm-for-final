package com.ititon.jdbc_orm.processor.listener.event;

import java.sql.Connection;
import java.sql.ResultSet;

public class SelectEvent extends Event {

    public enum Type {
        SELECT, SELECT_BY_ID, SELECT_WITH_LIMIT
    }
    private Type type;
    private Class<?> entityClass;
    private Integer skip;
    private Integer count;
    private Object id;

    public SelectEvent(Connection connection, Class<?> entityClass) {
        super(connection);
        this.entityClass = entityClass;
        this.type = Type.SELECT;
    }

    public SelectEvent(Connection connection, Class<?> entityClass, Object id) {
        super(connection);
        this.entityClass = entityClass;
        this.id = id;
        this.type = Type.SELECT_BY_ID;
    }

    public SelectEvent(Connection connection, Class<?> entityClass, Integer skip, Integer count) {
        super(connection);
        this.entityClass = entityClass;
        this.skip = skip;
        this.count = count;
        this.type = Type.SELECT_WITH_LIMIT;
    }

    public SelectEvent(Connection connection, Type type, Class<?> entityClass, Integer skip, Integer count, Integer id) {
        super(connection);
        this.type = type;
        this.entityClass = entityClass;
        this.skip = skip;
        this.count = count;
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public Integer getSkip() {
        return skip;
    }

    public Integer getCount() {
        return count;
    }

    public Object getId() {
        return id;
    }
}
