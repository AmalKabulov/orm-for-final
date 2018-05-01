package com.ititon.jdbc_orm.processor.listener.query_builder;

import com.ititon.jdbc_orm.processor.listener.info.TableInfo;

import java.util.Map;

public abstract class InsertQuery {

    public static String buildInsertQuery(final TableInfo tableInfo) {
        String tableName = tableInfo.getTableName();
        Map<String, String> columnsValues = tableInfo.getColumnsValues();
        String columns = String.join(", ", columnsValues.keySet());
        String values = String.join(", ", columnsValues.values());
        return "insert into " + tableName + "(" + columns + ") values (" + values + ")";

    }


}
