package com.ititon.jdbc_orm.processor.listener;

import com.ititon.jdbc_orm.ProcessedObject;
import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.processor.listener.event.SelectEvent;
import com.ititon.jdbc_orm.processor.listener.info.EntityInfo;
import com.ititon.jdbc_orm.processor.listener.query_builder.SelectQuery;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SelectEventListener implements EventListener<SelectEvent>{
    private CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    @Override
    public void execute(SelectEvent event) throws SQLException, DefaultOrmException {
        SelectEvent.Type type = event.getType();
        Connection connection = event.getConnection();
        Class<?> entityClass = event.getEntityClass();
        EntityMeta entityMeta = cacheProcessor.getMeta(entityClass);
        if (entityMeta == null) {
            return;
        }
        String query = null;
        if (type.equals(SelectEvent.Type.SELECT)) {
            query = SelectQuery.buildSelectQuery(entityClass);
        } else if (type.equals(SelectEvent.Type.SELECT_BY_ID)) {
            query = SelectQuery.buildSelectByIdQuery(entityClass, event.getId());
        } else if (type.equals(SelectEvent.Type.SELECT_WITH_LIMIT)) {
            query = SelectQuery.buildSelectByLimitQuery(entityClass, event.getSkip(), event.getCount());
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            ResultSet resultSet = preparedStatement.executeQuery();
            onSelect(entityMeta, resultSet);
        }
    }

    /**
     * method for correct parsing resultset
     * and adding result to entity cache
     *
     * @param entityMeta
     * @param resultSet
     * @throws SQLException
     */
    public void onSelect(final EntityMeta entityMeta, final ResultSet resultSet) throws SQLException {
        Object entity = null;

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

        if (entityInfo.getCurrentFieldMeta().getAnnotations().containsKey(Column.class)) {
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

        String fieldName = fieldMeta.getFieldName();
        ProcessedObject processedObject = new ProcessedObject(mainEntity.getClass(), fieldName);


        entityInfo.getProcessedObjects().add(processedObject);
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


}
