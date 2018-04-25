package com.ititon.jdbc_orm.processor.action;

import com.ititon.jdbc_orm.ProcessedObject;
import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.event.SelectEvent;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SelectEventListener {
    private CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    /**
     * method for correct parsing resultset
     * and adding result to entity cache
     *
     * @param selectEvent
     * @throws SQLException
     */
    public void onSelect(final SelectEvent selectEvent) throws SQLException {
        Object entity = null;
        Class<?> entityClass = selectEvent.getEntityClass();
        ResultSet resultSet = selectEvent.getResultSet();
        EntityMeta entityMeta = cacheProcessor.getMeta(entityClass);
        if (entityMeta == null) {
            return;
        }


        String tableName = entityMeta.getTableName();
        String idColumnName = tableName + "." + entityMeta.getIdColumnName();
        Class<?> idColumnType = entityMeta.getIdColumnType();
        String idColumnFieldName = entityMeta.getIdColumnFieldName();


        while (resultSet.next()) {

            Set<ProcessedObject> processedObjects = new HashSet<>();
            Object id = resultSet.getObject(idColumnName, idColumnType);
            if (entity == null || !ReflectionUtil.invokeGetter(entity, idColumnFieldName).equals(id)) {
                entity = ReflectionUtil.newInstance(entityMeta.getEntityClassName());
            }

            EntityInfo entityInfo = new EntityInfo(entity, entityMeta, processedObjects);
            fillEntity(resultSet, entityInfo);

            cacheProcessor.putEntity(entity);
        }
    }


    /**
     * fills already created entity
     * with values of resultset
     *
     * @param resultSet
     * @param entityInfo
     */
    private void fillEntity(final ResultSet resultSet,
                            final EntityInfo entityInfo) throws SQLException {


        EntityMeta mainEntityMeta = entityInfo.getMainEntityMeta();
        Collection<FieldMeta> fieldMetas = mainEntityMeta.getFieldMetas().values();

        for (FieldMeta fieldMeta : fieldMetas) {
            entityInfo.setCurrentFieldMeta(fieldMeta);
            processFieldMeta(resultSet, entityInfo);
        }

    }


    private void processFieldMeta(final ResultSet resultSet,
                                  final EntityInfo entityInfo) throws SQLException {

        if (entityInfo.currentFieldMeta.getAnnotations().containsKey(Column.class)) {
            processSimple(resultSet, entityInfo);
        } else {
            processComplex(resultSet, entityInfo);
        }
    }


    private void processSimple(final ResultSet resultSet,
                               final EntityInfo entityInfo) throws SQLException {

        EntityMeta entityMeta = entityInfo.getMainEntityMeta();
        String tableName = entityMeta.getTableName();
        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Class<?> fieldType = fieldMeta.getFieldType();
        String columnName = tableName + "." + fieldMeta.getColumnName();

        Object result = resultSet.getObject(columnName, fieldType);
        if (result != null) {
            ReflectionUtil.invokeSetter(entityInfo.getMainEntity(), fieldMeta.getFieldName(), result);
        }
    }


    private void processComplex(final ResultSet resultSet,
                                final EntityInfo entityInfo) throws SQLException {

        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();

        Class<?> entityClass = fieldMeta.getFieldGenericType();
        if (entityClass == null) {
            entityClass = fieldMeta.getFieldType();
        }

        Object mainEntity = entityInfo.getMainEntity();
        EntityMeta joinEntityMeta = cacheProcessor.getMeta(entityClass);
        Object joinEntity = ReflectionUtil.newInstance(joinEntityMeta.getEntityClassName());

        entityInfo.getProcessedObjects().add(new ProcessedObject(mainEntity.getClass(), fieldMeta.getFieldName()));
        entityInfo.setJoinEntityMeta(joinEntityMeta);
        entityInfo.setJoinEntity(joinEntity);

        Map<Class<? extends Annotation>, Annotation> fieldAnnotations = fieldMeta.getAnnotations();

        if (fieldAnnotations.containsKey(ManyToMany.class)) {
            handleManyToMany(resultSet, entityInfo);
        } else if (fieldAnnotations.containsKey(OneToMany.class)) {
            handleOneToMany(resultSet, entityInfo);
        } else if (fieldAnnotations.containsKey(OneToOne.class)) {
            handleOneToOne(resultSet, entityInfo);
        } else if (fieldAnnotations.containsKey(ManyToOne.class)) {
            handleManyToOne(resultSet, entityInfo);
        }
    }


    private void handleOneToMany(final ResultSet resultSet,
                                 final EntityInfo entityInfo) throws SQLException {

        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
        OneToMany manyToMany = (OneToMany) annotations.get(OneToMany.class);
        FetchType fetchType = manyToMany.fetch();
        if (fetchType.equals(FetchType.EAGER)) {

            EntityInfo joinEntityInfo = new EntityInfo(entityInfo.getJoinEntity(),
                                                       entityInfo.getJoinEntityMeta(),
                                                       entityInfo.getProcessedObjects());

            fillEntity(resultSet, joinEntityInfo);
            addEntityToCollectionOfMainEntity(entityInfo);
        }
    }




    private void handleManyToMany(final ResultSet resultSet,
                                  final EntityInfo entityInfo) throws SQLException {
        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
        ManyToMany manyToMany = (ManyToMany) annotations.get(ManyToMany.class);
        FetchType fetchType = manyToMany.fetch();
        if (fetchType.equals(FetchType.EAGER)) {

            EntityInfo joinEntityInfo = new EntityInfo(entityInfo.getJoinEntity(),
                                                       entityInfo.getJoinEntityMeta(),
                                                       entityInfo.getProcessedObjects());

            fillEntity(resultSet, joinEntityInfo);
            addEntityToCollectionOfMainEntity(entityInfo);
        }
    }

    private void handleOneToOne(final ResultSet resultSet,
                                final EntityInfo entityInfo) throws SQLException {

        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
        OneToOne oneToOne = (OneToOne) annotations.get(OneToOne.class);
        FetchType fetchType = oneToOne.fetch();

        if (fetchType.equals(FetchType.EAGER)) {
            EntityInfo joinEntityInfo = new EntityInfo(entityInfo.getJoinEntity(),
                                                       entityInfo.getJoinEntityMeta(),
                                                       entityInfo.getProcessedObjects());

            fillEntity(resultSet, joinEntityInfo);
            addEntityToMainEntity(entityInfo);
        }

    }

    private void handleManyToOne(final ResultSet resultSet,
                                 final EntityInfo entityInfo) throws SQLException {


        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
        ManyToOne oneToOne = (ManyToOne) annotations.get(ManyToOne.class);
        FetchType fetchType = oneToOne.fetch();

        if (fetchType.equals(FetchType.EAGER)) {
            EntityInfo joinEntityInfo = new EntityInfo(entityInfo.getJoinEntity(),
                    entityInfo.getJoinEntityMeta(),
                    entityInfo.getProcessedObjects());

            fillEntity(resultSet, joinEntityInfo);
            addEntityToMainEntity(entityInfo);
        }

    }


    private void addEntityToCollectionOfMainEntity(final EntityInfo entityInfo) {
        Object joinEntity = entityInfo.getJoinEntity();
        EntityMeta joinEntityMeta = entityInfo.getJoinEntityMeta();

        Object id = ReflectionUtil.invokeGetter(joinEntity, joinEntityMeta.getIdColumnFieldName());

        if (id != null && !Objects.equals(id, 0L)) {
            ///////**** Здесь вызывается геттер из оновной ентити ****\\\\\\\\\
            Object collection = ReflectionUtil.invokeGetter(entityInfo.getMainEntity(),
                                                            entityInfo.getCurrentFieldMeta().getFieldName());

            Method collectionAddMethod = ReflectionUtil.getMethod(collection.getClass(), "add", Object.class);
            ReflectionUtil.invokeMethod(collection, collectionAddMethod, joinEntity);
        }

    }


    private void addEntityToMainEntity(final EntityInfo entityInfo) {
        Object joinEntity = entityInfo.getJoinEntity();
        EntityMeta joinEntityMeta = entityInfo.getJoinEntityMeta();

        Object id = ReflectionUtil.invokeGetter(joinEntity, joinEntityMeta.getIdColumnFieldName());
        if (id != null && !Objects.equals(id, 0L)) {
            Object entity = entityInfo.getMainEntity();
            FieldMeta field = entityInfo.getCurrentFieldMeta();
            ReflectionUtil.invokeSetter(entity, field.getFieldName(), joinEntity);
        }
    }


    private class EntityInfo {

        private Object mainEntity;
        private EntityMeta mainEntityMeta;
        private Object joinEntity;
        private EntityMeta joinEntityMeta;
        private FieldMeta currentFieldMeta;
        private Set<ProcessedObject> processedObjects;


        public EntityInfo(Object mainEntity, EntityMeta mainEntityMeta, Object joinEntity, EntityMeta joinEntityMeta, FieldMeta currentFieldMeta, Set<ProcessedObject> processedObjects) {
            this.mainEntity = mainEntity;
            this.mainEntityMeta = mainEntityMeta;
            this.joinEntity = joinEntity;
            this.joinEntityMeta = joinEntityMeta;
            this.currentFieldMeta = currentFieldMeta;
            this.processedObjects = processedObjects;
        }

        public EntityInfo(Object mainEntity, EntityMeta mainEntityMeta, Set<ProcessedObject> processedObjects) {
            this.mainEntity = mainEntity;
            this.mainEntityMeta = mainEntityMeta;
            this.processedObjects = processedObjects;
        }

        public EntityInfo() {
        }

        public Object getMainEntity() {
            return mainEntity;
        }

        public void setMainEntity(Object mainEntity) {
            this.mainEntity = mainEntity;
        }

        public EntityMeta getMainEntityMeta() {
            return mainEntityMeta;
        }

        public void setMainEntityMeta(EntityMeta mainEntityMeta) {
            this.mainEntityMeta = mainEntityMeta;
        }

        public Object getJoinEntity() {
            return joinEntity;
        }

        public void setJoinEntity(Object joinEntity) {
            this.joinEntity = joinEntity;
        }

        public EntityMeta getJoinEntityMeta() {
            return joinEntityMeta;
        }

        public void setJoinEntityMeta(EntityMeta joinEntityMeta) {
            this.joinEntityMeta = joinEntityMeta;
        }

        public FieldMeta getCurrentFieldMeta() {
            return currentFieldMeta;
        }

        public void setCurrentFieldMeta(FieldMeta currentFieldMeta) {
            this.currentFieldMeta = currentFieldMeta;
        }

        public Set<ProcessedObject> getProcessedObjects() {
            return processedObjects;
        }

        public void setProcessedObjects(Set<ProcessedObject> processedObjects) {
            this.processedObjects = processedObjects;
        }
    }


}
