package com.ititon.jdbc_orm.processor.event.info;

import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.action.InsertEventListener;
import com.ititon.jdbc_orm.processor.event.InsertEvent;

import java.sql.Connection;
import java.util.List;
import java.util.Set;

public class InsertEventInfo {
    private Object entity;
    private List<JoinTableInfo> joinTablesInfo;
    private Set<Object> processedObjects;
//        private Map<String, String> columnsValues = new LinkedHashMap<>();

    private Object getterResult;
    private FieldMeta currentFieldMeta;
    private Connection connection;

    private InsertEvent.Type type;


    public InsertEventInfo(Object entity,
                           List<JoinTableInfo> joinTablesInfo,
                           Set<Object> processedObjects,
                           Connection connection,
                           InsertEvent.Type type) {
        this.entity = entity;
        this.joinTablesInfo = joinTablesInfo;
        this.processedObjects = processedObjects;
        this.connection = connection;
        this.type = type;
    }

    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public List<JoinTableInfo> getJoinTablesInfo() {
        return joinTablesInfo;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setJoinTablesInfo(List<JoinTableInfo> joinTablesInfo) {
        this.joinTablesInfo = joinTablesInfo;
    }

    public Set<Object> getProcessedObjects() {
        return processedObjects;
    }

    public void setProcessedObjects(Set<Object> processedObjects) {
        this.processedObjects = processedObjects;
    }

    public Object getGetterResult() {
        return getterResult;
    }

    public void setGetterResult(Object getterResult) {
        this.getterResult = getterResult;
    }
    //
//        public Map<String, String> getColumnsValues() {
//            return columnsValues;
//        }
//
//        public void setColumnsValues(Map<String, String> columnsValues) {
//            this.columnsValues = columnsValues;
//        }


    public InsertEvent.Type getType() {
        return type;
    }

    public FieldMeta getCurrentFieldMeta() {
        return currentFieldMeta;
    }

    public void setCurrentFieldMeta(FieldMeta currentFieldMeta) {
        this.currentFieldMeta = currentFieldMeta;
    }
}
