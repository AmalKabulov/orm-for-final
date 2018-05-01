package com.ititon.jdbc_orm.processor.listener.query_builder;

import com.ititon.jdbc_orm.processor.listener.info.TableInfo;

public abstract class UpdateQuery {


    public static String buildUpdateQuery(final TableInfo tableInfo) {
        StringBuilder query = new StringBuilder("update ").append(tableInfo.getTableName()).append(" set ");
        StringBuilder columnsValuesLikeString = new StringBuilder();
        tableInfo.getColumnsValues().forEach((k, v) -> columnsValuesLikeString.append(k)
                .append(" = ")
                .append(v)
                .append(", "));
        columnsValuesLikeString.setLength(columnsValuesLikeString.length() - 2);
        query.append(columnsValuesLikeString);
        if (tableInfo.isJoinTable()) {
            query.append(" where ").append(columnsValuesLikeString);
        } else {
            query.append(" where ").append(tableInfo.getIdColumnName()).append(" = ").append(tableInfo.getIdValue());
        }

        return query.append(";").toString();
    }
}
