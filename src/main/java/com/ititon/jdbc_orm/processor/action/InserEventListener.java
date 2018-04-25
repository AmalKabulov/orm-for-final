package com.ititon.jdbc_orm.processor.action;

import com.ititon.jdbc_orm.annotation.Column;
import com.ititon.jdbc_orm.annotation.Id;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.event.InsertEvent;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class InserEventListener {

    private final CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    public void onInsert(final InsertEvent event) {
        Object entity = event.getEntity();
        EntityMeta entityMeta = cacheProcessor.getMeta(entity.getClass());
        Map<String, String> columnsAndValues = getColumnsAndValues(entity, entityMeta);
        String columns = String.join(", ", columnsAndValues.keySet());
        String values = String.join(", ", columnsAndValues.values());

    }


    private Map<String, String> getColumnsAndValues(final Object entity, final EntityMeta entityMeta) {
        Collection<FieldMeta> fieldMetas = entityMeta.getFieldMetas().values();
        Map<String, String> columnsValues = new LinkedHashMap<>();
        for (FieldMeta fieldMeta : fieldMetas) {
            Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
            //если маны то маны то в коллекчию иначи excecuteupdate
            if (!annotations.containsKey(Id.class) && annotations.containsKey(Column.class)) {
                Object result = ReflectionUtil.invokeGetter(entity, fieldMeta.getFieldName());
                columnsValues.put(fieldMeta.getColumnName(), String.valueOf(result));
            }
        }
        return columnsValues;
    }
}
