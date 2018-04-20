package com.ititon.jdbc_orm.processor.parser;



import com.ititon.jdbc_orm.annotation.FetchType;
import com.ititon.jdbc_orm.annotation.ManyToMany;
import com.ititon.jdbc_orm.annotation.OneToMany;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Parser {

    private CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    public Object parseSimple(final Class<?> entityClass, final ResultSet resultSet) throws SQLException {
        Set<ProcessedObject> processedObjects = new HashSet<>();
        EntityMeta entityMeta = cacheProcessor.getMeta(entityClass);
        if (entityMeta == null) {
            return null;
        }
        Object entity = ReflectionUtil.newInstance(entityMeta.getEntityClassName());;
//        if (entity == null || !ReflectionUtil.invokeGetter(entity, idColumnFieldName).equals(id)) {
//            entity = ReflectionUtil.newInstance(entityMeta.getEntityClassName());
//        }

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

//                    if (field.getAnnotations().containsKey(ManyToMany.class)) {
//                        ManyToMany manyToMany = (ManyToMany) field.getAnnotations().get(ManyToMany.class);
//                        fillEntity(joinEntityMeta, joinEntity, resultSet, processedObjects);
//                        fillCollectionOf(entity, field, joinEntity, joinEntityMeta, manyToMany.fetch());
//                    } else if (field.getAnnotations().containsKey(OneToMany.class)) {
//                        OneToMany oneToMany = (OneToMany) field.getAnnotations().get(OneToMany.class);
//                        fillEntity(joinEntityMeta, joinEntity, resultSet, processedObjects);
//                        fillCollectionOf(entity, field, joinEntity, joinEntityMeta, oneToMany.fetch());
//                    }

                    if (field.getAnnotations().containsKey(ManyToMany.class)
                            || field.getAnnotations().containsKey(OneToMany.class)) {

                        FetchType fetchType = null;

                        ManyToMany manyToMany = (ManyToMany) field.getAnnotations().get(ManyToMany.class);

                        if (manyToMany != null) {
                            fetchType = manyToMany.fetch();
                        } else {
                            OneToMany oneToMany = (OneToMany) field.getAnnotations().get(OneToMany.class);
                            fetchType = oneToMany.fetch();
                        }

                        fillEntity(joinEntityMeta, joinEntity, resultSet, processedObjects);


                        if (fetchType.equals(FetchType.EAGER)) {
                            Object idGetter = ReflectionUtil.invokeGetter(joinEntity, joinEntityMeta.getIdColumnFieldName());

                            if (idGetter != null && !Objects.equals(idGetter, 0L)) {
                                ///////**** Здесь вызывается геттер из оновной ентити ****\\\\\\\\\
                                Object collection = ReflectionUtil.invokeGetter(entity, field.getFieldName());
                                Method collectionAddMethod = ReflectionUtil.getMethod(collection.getClass(), "add", Object.class);
                                ReflectionUtil.invokeMethod(collection, collectionAddMethod, joinEntity);
                            }
                        }
                    }
                }
            }
        }
    }


//    private void fillCollectionOf(Object fillableEntity, FieldMeta fillableEntityField, Object entityToAdd, EntityMeta entityToAddMeta, FetchType fetchType) {
//        if (fetchType.equals(FetchType.EAGER)) {
//            Object idGetter = ReflectionUtil.invokeGetter(entityToAdd, entityToAddMeta.getIdColumnFieldName());
//            if (idGetter != null && !Objects.equals(idGetter, 0L)) {
//                ///////**** Здесь вызывается геттер из оновной ентити ****\\\\\\\\\
//                Object collection = ReflectionUtil.invokeGetter(fillableEntity, fillableEntityField.getFieldName());
//                Method collectionAddMethod = ReflectionUtil.getMethod(collection.getClass(), "add", Object.class);
//                ReflectionUtil.invokeMethod(collection, collectionAddMethod, entityToAdd);
//            }
//        }
//    }

//    private FetchType getFetchType(final FieldMeta fieldMeta) {
//        ManyToMany manyToMany = (ManyToMany) fieldMeta.getAnnotations().get(ManyToMany.class);
//        FetchType fetchType;
//
//        if (manyToMany != null) {
//            fetchType = manyToMany.fetch();
//        } else {
//            OneToMany oneToMany = (OneToMany) fieldMeta.getAnnotations().get(OneToMany.class);
//            fetchType = oneToMany.fetch();
//        }
//        return fetchType;
//
//    }


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
