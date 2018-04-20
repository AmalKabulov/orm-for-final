package com.ititon.jdbc_orm.processor.parser;


import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ResultSetParser {

    private CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    public Object parseSimple(final Class<?> entityClass, final ResultSet resultSet) throws SQLException {
        Set<ProcessedObject> processedObjects = new HashSet<>();
        EntityMeta entityMeta = cacheProcessor.getMeta(entityClass);
        if (entityMeta == null) {
            return null;
        }
        Object entity = ReflectionUtil.newInstance(entityMeta.getEntityClassName());

        fillEntity(entityMeta, entity, resultSet, processedObjects);
        return entity;

    }


    public List<Object> parseComplex(final Class<?> entityClass, final ResultSet resultSet) throws SQLException {
        List<Object> entities = new ArrayList<>();
        Set<ProcessedObject> processedObjects = new HashSet<>();
        Object entity = null;
        EntityMeta entityMeta = cacheProcessor.getMeta(entityClass);
        if (entityMeta == null) {
            return null;
        }

        String idColumnName = entityMeta.getIdColumnName();
        String tableName = entityMeta.getTableName();
        Class<?> idColumnType = entityMeta.getIdColumnType();
        String idColumnFieldName = entityMeta.getIdColumnFieldName();


        while (resultSet.next()) {
            String entityId = tableName + "." + idColumnName;
            Object id = resultSet.getObject(entityId, idColumnType);

            if (entity == null || !ReflectionUtil.invokeGetter(entity, idColumnFieldName).equals(id)) {
                entity = ReflectionUtil.newInstance(entityMeta.getEntityClassName());
            }

            ReflectionUtil.invokeSetter(entity, idColumnFieldName, id);

            fillEntity(entityMeta, entity, resultSet, processedObjects);

            entities.add(entity);
        }

        return entities;
    }

    private void fillEntity(EntityMeta entityMeta, Object entity, ResultSet resultSet, Set<ProcessedObject> processedObjects) throws SQLException {

        String tableName = entityMeta.getTableName();
        Collection<FieldMeta> fields = entityMeta.getFieldMetas().values();

        for (FieldMeta field : fields) {

            if (!processedObjects.contains(new ProcessedObject(entity.getClass(), field.getFieldName()))) {
                String columnName = field.getColumnName();
                if (columnName != null) {
                    columnName = tableName + "." + columnName;
                    Object result = resultSet.getObject(columnName, field.getFieldType());
                    if (result != null) {
                        ReflectionUtil.invokeSetter(entity, field.getFieldName(), result);
                    }
                } else {
                    Class<?> joinEntityClass = field.getFieldGenericType();
                    EntityMeta joinEntityMeta = cacheProcessor.getMeta(joinEntityClass);
                    Object joinEntity = ReflectionUtil.newInstance(joinEntityMeta.getEntityClassName());
                    processedObjects.add(new ProcessedObject(entity.getClass(), field.getFieldName()));
                    Map<Class<? extends Annotation>, Annotation> fieldAnnotations = field.getAnnotations();

                    if (fieldAnnotations.containsKey(ManyToMany.class)
                            || fieldAnnotations.containsKey(OneToMany.class)) {

                        FetchType fetchType = null;

                        ManyToMany manyToMany = (ManyToMany) fieldAnnotations.get(ManyToMany.class);

                        if (manyToMany != null) {
                            fetchType = manyToMany.fetch();
                        } else {
                            OneToMany oneToMany = (OneToMany) fieldAnnotations.get(OneToMany.class);
                            fetchType = oneToMany.fetch();
                        }

                        if (fetchType.equals(FetchType.EAGER)) {
                            fillEntity(joinEntityMeta, joinEntity, resultSet, processedObjects);
                            Object id = ReflectionUtil.invokeGetter(joinEntity, joinEntityMeta.getIdColumnFieldName());

                            if (id != null && !Objects.equals(id, 0L)) {
                                ///////**** Здесь вызывается геттер из оновной ентити ****\\\\\\\\\
                                Object collection = ReflectionUtil.invokeGetter(entity, field.getFieldName());
                                Method collectionAddMethod = ReflectionUtil.getMethod(collection.getClass(), "add", Object.class);
                                ReflectionUtil.invokeMethod(collection, collectionAddMethod, joinEntity);
                            }
                        }
                    } else if (fieldAnnotations.containsKey(OneToOne.class) || fieldAnnotations.containsKey(ManyToOne.class)) {

                        FetchType fetchType = null;
                        OneToOne oneToOne = (OneToOne) fieldAnnotations.get(OneToOne.class);
                        if (oneToOne != null) {
                            fetchType = oneToOne.fetch();
                        } else {
                            ManyToOne manyToOne = (ManyToOne) fieldAnnotations.get(ManyToOne.class);
                            fetchType = manyToOne.fetch();
                        }

                        if (fetchType.equals(FetchType.EAGER)) {
                            fillEntity(joinEntityMeta, joinEntity, resultSet, processedObjects);
                            Object id = ReflectionUtil.invokeGetter(joinEntity, joinEntityMeta.getIdColumnFieldName());
                            if (id != null && !Objects.equals(id, 0L)) {
                                ReflectionUtil.invokeSetter(entity, field.getFieldName(), joinEntity);
                            }

                        }
                        
                    }
                }
            }
        }
    }


    private class ProcessedObject {
        private Class<?> entityClass;
        private String fieldName;


        public ProcessedObject(Class<?> entityClass, String fieldName) {
            this.entityClass = entityClass;
            this.fieldName = fieldName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProcessedObject)) return false;
            ProcessedObject processedObject = (ProcessedObject) o;
            return Objects.equals(entityClass, processedObject.entityClass) &&
                    Objects.equals(fieldName, processedObject.fieldName);
        }

        @Override
        public int hashCode() {

            return Objects.hash(entityClass, fieldName);
        }
    }
}
