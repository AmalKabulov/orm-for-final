package com.ititon.jdbc_orm.processor.action;

import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.event.InsertEvent;
import com.ititon.jdbc_orm.processor.event.info.InsertEventInfo;
import com.ititon.jdbc_orm.processor.event.info.JoinTableInfo;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.sql.*;
import java.util.*;

public class InsertEventListener {

    private final CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    public void onInsert(final InsertEvent event) throws SQLException, DefaultOrmException {
        Object entity = event.getEntity();
        Set<Object> processedObjects = new HashSet<>();

        List<JoinTableInfo> joinTablesInfo = new ArrayList<>();
        InsertEventInfo insertEventInfo = new InsertEventInfo(entity, joinTablesInfo, processedObjects, event.getConnection());
        try {
            onInsert(insertEventInfo);
            generateAndExecuteJoinTableQueries(insertEventInfo);
        } catch (SQLException e) {
            event.getConnection().rollback();
           throw new DefaultOrmException("Error while saving entity : " + entity);
        }

    }



    private void generateAndExecuteJoinTableQueries(final InsertEventInfo info) throws SQLException {
        List<JoinTableInfo> joinTablesInfo = info.getJoinTablesInfo();
        Connection connection = info.getConnection();
        for (JoinTableInfo joinTableInfo : joinTablesInfo) {

            Object mainEntity = joinTableInfo.getMainEntity();
            Object innerEntity = joinTableInfo.getInnerEntity();

            EntityMeta mainMeta = cacheProcessor.getMeta(mainEntity.getClass());
            EntityMeta innerMeta = cacheProcessor.getMeta(innerEntity.getClass());
            Object mainId = ReflectionUtil.invokeGetter(mainEntity, mainMeta.getIdColumnFieldName());
            Object innerId = ReflectionUtil.invokeGetter(innerEntity, innerMeta.getIdColumnFieldName());
            String joinQuery = "insert into " + joinTableInfo.getJoinTableName() +
                    "(`" + joinTableInfo.getMainEntityIdColumnName() + "` , `" + joinTableInfo.getInnerEntityIdColumnName() + "`)"
                    + " values ('" + mainId + "' , '" + innerId + "')";

            try (PreparedStatement preparedStatement = connection.prepareStatement(joinQuery)) {
                preparedStatement.executeUpdate();
            }

            System.out.println(joinQuery);
        }
    }


    private void onInsert(final InsertEventInfo info) throws SQLException {

        Object entity = info.getEntity();
        Map<String, String> columnsValues = new LinkedHashMap<>();
        if (info.getProcessedObjects().contains(entity)) {
            return;
        }
        info.getProcessedObjects().add(entity);
        handleFieldsMeta(info, columnsValues);
        generateAndInvokeInsertQuery(info, columnsValues);
    }


    private void handleFieldsMeta(final InsertEventInfo info, final Map<String, String> columnsValues) throws SQLException {
        Object entity = info.getEntity();
        EntityMeta entityMeta = cacheProcessor.getMeta(entity.getClass());
        Collection<FieldMeta> fieldMetas = entityMeta.getFieldMetas().values();

        for (FieldMeta fieldMeta : fieldMetas) {
            Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
            Object result = ReflectionUtil.invokeGetter(entity, fieldMeta.getFieldName());

            info.setCurrentFieldMeta(fieldMeta);
            info.setGetterResult(result);

            if (!annotations.containsKey(Id.class) && annotations.containsKey(Column.class)) {
                columnsValues.put(fieldMeta.getColumnName(), wrap(String.valueOf((Object) result)));
            } else if (annotations.containsKey(ManyToMany.class)) {
                handleFieldWithManyToManyAnnotation(info);
            } else if (annotations.containsKey(OneToMany.class)) {
                handleFieldWithOneToManyAssociation(info);
            } else if (annotations.containsKey(ManyToOne.class)) {
                handleFieldWithManyToOneAssociation(info, columnsValues);
            } else if (annotations.containsKey(OneToOne.class)) {
                handleOneToOneAssociation(info, columnsValues);
            }
        }

    }

    private void generateAndInvokeInsertQuery(final InsertEventInfo info,
                                    final Map<String, String> columnsValues) throws SQLException {

        Object entity = info.getEntity();
        EntityMeta entityMeta = cacheProcessor.getMeta(entity.getClass());
        Object entityId = ReflectionUtil.invokeGetter(entity, entityMeta.getIdColumnFieldName());

        if (entityId == null) {
            String columns = String.join(", ", columnsValues.keySet());
            String values = String.join(", ", columnsValues.values());

            String insertQuery = "insert into " +
                    entityMeta.getTableName() +
                    " (" + columns +
                    ") values (" +
                    values + ")" +
                    ";";

            System.out.println("INSERT QUERY: " + insertQuery);
            Connection connection = info.getConnection();
            try (PreparedStatement preparedStatement =
                         connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.executeUpdate();
                ResultSet resultSet = preparedStatement.getGeneratedKeys();
                while (resultSet.next()) {
                    Object id = resultSet.getObject(1, entityMeta.getIdColumnType());
                    ReflectionUtil.invokeSetter(entity, entityMeta.getIdColumnFieldName(), id);

                }
            }
        }
    }


    private void handleOneToOneAssociation(final InsertEventInfo info,
                                           final Map<String, String> columnsValues) throws SQLException {

        FieldMeta fieldMeta = info.getCurrentFieldMeta();
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
        OneToOne oneToOne = (OneToOne) annotations.get(OneToOne.class);
        CascadeType[] cascade = oneToOne.cascade();
        List<CascadeType> cascadeTypes = Arrays.asList(cascade);
        boolean contains = cascadeTypes.contains(CascadeType.SAVE_UPDATE) || cascadeTypes.contains(CascadeType.ALL) ;
        if (!contains) {
            return;
        }

        String mappedFieldName = oneToOne.mappedBy();
        if (mappedFieldName.isEmpty()) {
            JoinColumn joinColumn = (JoinColumn) annotations.get(JoinColumn.class);
            handleGetterResult(info, joinColumn, columnsValues);
        } else {
            InsertEventInfo newInfo =
                    new InsertEventInfo(info.getGetterResult(), info.getJoinTablesInfo(), info.getProcessedObjects(), info.getConnection());
            onInsert(newInfo);

        }
    }

    private void handleGetterResult(final InsertEventInfo info,
                                    final JoinColumn joinColumn,
                                    final Map<String, String> columnsValues) throws SQLException {
        Object getterResult = info.getGetterResult();
        Object id = null;
        if (getterResult != null) {
            EntityMeta entityMeta = cacheProcessor.getMeta(getterResult.getClass());
            id = ReflectionUtil.invokeGetter(getterResult, entityMeta.getIdColumnFieldName());
            if (id != null) {
                columnsValues.put(joinColumn.name(), wrap(String.valueOf(id)));
            } else {
                InsertEventInfo newInfo =
                        new InsertEventInfo(getterResult, info.getJoinTablesInfo(), info.getProcessedObjects(), info.getConnection());
                onInsert(newInfo);
                id = ReflectionUtil.invokeGetter(getterResult, entityMeta.getIdColumnFieldName());
                columnsValues.put(joinColumn.name(), wrap(String.valueOf(id)));
            }
        }
    }


    private void handleFieldWithOneToManyAssociation(final InsertEventInfo info) throws SQLException {


        FieldMeta fieldMeta1 = info.getCurrentFieldMeta();
        OneToMany oneToMany = (OneToMany) fieldMeta1.getAnnotations().get(OneToMany.class);
        CascadeType[] cascade = oneToMany.cascade();
        List<CascadeType> cascadeTypes = Arrays.asList(cascade);
        boolean contains = cascadeTypes.contains(CascadeType.SAVE_UPDATE) || cascadeTypes.contains(CascadeType.ALL) ;
        if (!contains) {
            return;
        }

        Collection collectionOfJoinEntities = null;
        Object getterResult1 = info.getGetterResult();
        if (getterResult1 instanceof Collection) {
            collectionOfJoinEntities = (Collection) getterResult1;
        }

        if (collectionOfJoinEntities != null && !collectionOfJoinEntities.isEmpty()) {
            for (Object entity : collectionOfJoinEntities) {
                InsertEventInfo insertEventInfo =
                        new InsertEventInfo(entity, info.getJoinTablesInfo(), info.getProcessedObjects(), info.getConnection());
                onInsert(insertEventInfo);
            }
        }

    }

    private void handleFieldWithManyToOneAssociation(final InsertEventInfo info,
                                                     final Map<String, String> columnsValues) throws SQLException {

        FieldMeta fieldMeta1 = info.getCurrentFieldMeta();
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta1.getAnnotations();
        ManyToOne manyToOne = (ManyToOne) annotations.get(ManyToOne.class);
        CascadeType[] cascade = manyToOne.cascade();
        List<CascadeType> cascadeTypes = Arrays.asList(cascade);
        boolean contains = cascadeTypes.contains(CascadeType.SAVE_UPDATE) || cascadeTypes.contains(CascadeType.ALL) ;
        if (!contains) {
            return;
        }

        JoinColumn joinColumn = (JoinColumn) annotations.get(JoinColumn.class);
        handleGetterResult(info, joinColumn, columnsValues);
    }

    public void handleFieldWithManyToManyAnnotation(final InsertEventInfo info) throws SQLException {


        FieldMeta fieldMeta1 = info.getCurrentFieldMeta();
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta1.getAnnotations();
        ManyToMany manyToMany = (ManyToMany) annotations.get(ManyToMany.class);
        String mappedFieldName = manyToMany.mappedBy();
        CascadeType[] cascade = manyToMany.cascade();

        List<CascadeType> cascadeTypes = Arrays.asList(cascade);
        boolean contains = cascadeTypes.contains(CascadeType.SAVE_UPDATE) || cascadeTypes.contains(CascadeType.ALL) ;
        if (!contains) {
            return;
        }

        /** можно вынести в отдельный метод*/

        Class<?> fieldGenericType = fieldMeta1.getFieldGenericType();
        EntityMeta joinEntityMeta = cacheProcessor.getMeta(fieldGenericType);

        JoinTable joinTable = null;
        boolean reverse = false;


        if (mappedFieldName.isEmpty()) {
            joinTable = (JoinTable) annotations.get(JoinTable.class);
        } else {
            Collection<FieldMeta> joinEntityFieldMetas = joinEntityMeta.getFieldMetas().values();
            FieldMeta joinEntityFieldMeta = findFieldMetaByMappedFieldName(joinEntityFieldMetas, mappedFieldName);
            joinTable = (JoinTable) joinEntityFieldMeta.getAnnotations().get(JoinTable.class);
            reverse = true;
        }


        Collection collectionOfJoinEntities = null;
        Object getterResult1 = info.getGetterResult();
        if (getterResult1 instanceof Collection) {
            collectionOfJoinEntities = (Collection) getterResult1;
        }

        if (collectionOfJoinEntities != null && !collectionOfJoinEntities.isEmpty()) {
            for (Object innerEntity : collectionOfJoinEntities) {
                JoinTableInfo joinTableInfo = createJoinTableInfo(info.getEntity(), innerEntity, joinTable, reverse);
                List<JoinTableInfo> joinTablesInfo = info.getJoinTablesInfo();
                joinTablesInfo.add(joinTableInfo);
                InsertEventInfo newInfo = new InsertEventInfo(innerEntity, joinTablesInfo, info.getProcessedObjects(), info.getConnection());
                onInsert(newInfo);
            }
        }
    }


    private JoinTableInfo createJoinTableInfo(final Object mainEntity,
                                              final Object innerEntity,
                                              JoinTable joinTable,
                                              final boolean reverse) {

        String joinTableName = joinTable.name();
        String joinColumn = joinTable.joinColumns()[0].name();
        String inverseJoinColumn = joinTable.inverseJoinColumns()[0].name();


        if (reverse) {
            return new JoinTableInfo(joinTableName, mainEntity, inverseJoinColumn, innerEntity, joinColumn);
        }

        return new JoinTableInfo(joinTableName, innerEntity, inverseJoinColumn, mainEntity, joinColumn);

    }


    /**
     * Finds field meta by mappedby value (which takes
     * from annotation ManyToMany, OneToMany and OneToOne)
     * from collection of field metas;
     *
     * @param fieldMetas
     * @param mappedFieldName
     * @return
     * @throws DefaultOrmException
     */
    private FieldMeta findFieldMetaByMappedFieldName(final Collection<FieldMeta> fieldMetas, final String mappedFieldName) {
        return fieldMetas
                .stream()
                .filter(f -> f.getFieldName().equals(mappedFieldName)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Mapped field: " + mappedFieldName + " not found"));
    }

    private static String wrap(String value) {
        return "\'" + value + "\'";
    }


}
