package com.ititon.jdbc_orm.processor.listener.info;

import java.util.Map;

public class TableInfo {
    private String tableName;
    private String idColumnName;
    private Object idValue;
    private Map<String, String> columnsValues;
    private boolean isJoinTable;

    public TableInfo() {
    }

    public TableInfo(String tableName,
                     String idColumnName,
                     Object idValue,
                     Map<String, String> columnsValues,
                     boolean isJoinTable) {
        this.tableName = tableName;
        this.idColumnName = idColumnName;
        this.idValue = idValue;
        this.columnsValues = columnsValues;
        this.isJoinTable = isJoinTable;
    }

    public TableInfo(String tableName, Map<String, String> columnsValues, boolean isJoinTable) {
        this.tableName = tableName;
        this.columnsValues = columnsValues;
        this.isJoinTable = isJoinTable;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    public void setIdColumnName(String idColumnName) {
        this.idColumnName = idColumnName;
    }

    public Object getIdValue() {
        return idValue;
    }

    public void setIdValue(Object idValue) {
        this.idValue = idValue;
    }

    public Map<String, String> getColumnsValues() {
        return columnsValues;
    }

    public void setColumnsValues(Map<String, String> columnsValues) {
        this.columnsValues = columnsValues;
    }

    public boolean isJoinTable() {
        return isJoinTable;
    }

    public void setJoinTable(boolean joinTable) {
        isJoinTable = joinTable;
    }
}
