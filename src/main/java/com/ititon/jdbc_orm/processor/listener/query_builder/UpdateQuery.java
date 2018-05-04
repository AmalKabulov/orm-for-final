package com.ititon.jdbc_orm.processor.listener.query_builder;

import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.listener.info.TableInfo;
import com.ititon.jdbc_orm.util.ReflectionUtil;

public abstract class UpdateQuery {
    private static final CacheProcessor CACHE_PROCESSOR = CacheProcessor.getInstance();


    public static String buildUpdateQuery(final TableInfo tableInfo) {
        StringBuilder query = new StringBuilder("update ").append(tableInfo.getTableName()).append(" set ");
        StringBuilder columnsValuesLikeString = new StringBuilder();
        tableInfo.getColumnsValues().forEach((k, v) -> columnsValuesLikeString.append(k)
                .append(" = ")
                .append(v)
                .append(", "));
        columnsValuesLikeString.setLength(columnsValuesLikeString.length() - 2);
        query.append(columnsValuesLikeString);

        Object id = tableInfo.getIdValue();
        EntityMeta innerEntity = CACHE_PROCESSOR.getMeta(id.getClass());
        if (innerEntity != null) {
            id = ReflectionUtil.invokeGetter(id, innerEntity.getIdColumnFieldName());
        }

        if (tableInfo.isJoinTable()) {
            query.append(" where ").append(columnsValuesLikeString);
        } else {
            query.append(" where ").append(tableInfo.getIdColumnName()).append(" = ").append(id);
        }

        return query.append(";").toString();
    }
}
