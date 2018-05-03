package com.ititon.jdbc_orm.processor.listener;

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


/** SelectEvenListener - этот класс слушает event.
 * Обращается к SelectQuery который герерирует запрос.
 * Также взависимости от event`а он генерируется подходящий запрос,
 * например select by id, select with limit - ограниченный селектб к примеру
 * чтоб достать 10 значений из базы(Select `columns` from `tablename` limit `from`, `count`)
 * и обычный селект.
 *
 */
public class SelectEventListener implements EventListener<SelectEvent> {

    /**
     * Кеш прооцессор который может работать с метаданными;
     */
    private CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    /**
     * Метод для обработки event`а. В нем же генерируются подходящие запросы,
     * берется коннекш из пула, создается preparedstatement c сгенерированным
     * ранее запросом и выполныется.
     * Также метод распарсивает полученный resultset, создает и заполняет его данными из
     * resultset`а
     * @param event
     * @throws SQLException
     * @throws DefaultOrmException
     */
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

        System.out.println("SELECT QUERY IS: " + query);
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
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
            Map<String, HashSet<String>> processedObjectFields = new HashMap<>();
//            Set<ProcessedObject> processedObjects = new HashSet<>();
//            Set<ProcessedObject> processedObjects = new HashSet<>();
            Object id = resultSet.getObject(idColumnName, idColumnType);
            if (entity == null || !ReflectionUtil.invokeGetter(entity, idColumnFieldName).equals(id)) {
                entity = ReflectionUtil.newInstance(entityMeta.getEntityClassName());
            }

            EntityInfo entityInfo = new EntityInfo(entity, entityMeta, processedObjectFields);
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



//        String fieldName = fieldMeta.getFieldName();
        if (isObjectProcessed(entityInfo)) {
            return;
        }

        if (!isFieldProcessed(entityInfo)) {
            deleteProcessedObject(entityInfo);
        }

        addToProcessedObjects(entityInfo);
//        ProcessedObject processedObject = new ProcessedObject(mainEntity.getClass(), fieldName);
//        entityInfo.getProcessedObjects().add(processedObject);

        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Class<?> entityClass = fieldMeta.getFieldGenericType();
        if (entityClass == null) {
            entityClass = fieldMeta.getFieldType();
        }

        EntityMeta joinEntityMeta = cacheProcessor.getMeta(entityClass);
        Object joinEntity = ReflectionUtil.newInstance(joinEntityMeta.getEntityClassName());
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
                    entityInfo.getProcessedObjectFields());

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
                    entityInfo.getProcessedObjectFields());

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
                    entityInfo.getProcessedObjectFields());

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
                    entityInfo.getProcessedObjectFields());

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


    private boolean isObjectProcessed(final EntityInfo entityInfo) {
        Object entity = entityInfo.getMainEntity();
        String entityName = entity.getClass().getName();
        Map<String, HashSet<String>> processedObjectFields = entityInfo.getProcessedObjectFields();
        return processedObjectFields.containsKey(entityName);
    }

    private void addToProcessedObjects(final EntityInfo entityInfo) {
        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Object entity = entityInfo.getMainEntity();
        String entityName = entity.getClass().getName();
        String fieldName = fieldMeta.getFieldName();
        Map<String, HashSet<String>> processedObjectFields = entityInfo.getProcessedObjectFields();

        if (isObjectProcessed(entityInfo)) {
            processedObjectFields.keySet().add(fieldName);
        } else {
            HashSet<String> fields = new HashSet<>();
            fields.add(fieldName);
            processedObjectFields.put(entityName, fields);
        }

    }

    private boolean isFieldProcessed(final EntityInfo entityInfo) {
        FieldMeta fieldMeta = entityInfo.getCurrentFieldMeta();
        Object entity = entityInfo.getMainEntity();
        String entityName = entity.getClass().getName();
        String fieldName = fieldMeta.getFieldName();
        Map<String, HashSet<String>> processedObjectFields = entityInfo.getProcessedObjectFields();

        if (isObjectProcessed(entityInfo)) {
            return processedObjectFields.get(entityName).contains(fieldName);
        }
        return false;
    }

    private void deleteProcessedObject(final EntityInfo entityInfo) {
        Object entity = entityInfo.getMainEntity();
        String entityName = entity.getClass().getName();

        entityInfo.getProcessedObjectFields().remove(entityName);
    }
}
