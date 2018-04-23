package com.ititon.jdbc_orm.processor.query_builder;

import com.ititon.jdbc_orm.ProcessedObject;
import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.util.*;

public abstract class InsertQuery {
    private static final CacheProcessor CACHE_PROCESSOR = CacheProcessor.getInstance();

    public static List<String> buildInsertQuery(final Object object) throws DefaultOrmException {
        Set<ProcessedObject> processedEntities = new HashSet<>();
        List<String> insertQueries = new ArrayList<>();
        List<String> joinQueries = new ArrayList<>();

        buildComplexInsertQuery(object, insertQueries, joinQueries, processedEntities);
        insertQueries.addAll(joinQueries);
        return insertQueries;
    }


    private static void buildComplexInsertQuery(final Object entity,
                                                final List<String> insertQueries,
                                                final List<String> joinqueries,
                                                final Set<ProcessedObject> processedObjects) throws DefaultOrmException {

        EntityMeta mainEntityMeta = CACHE_PROCESSOR.getMeta(entity.getClass());
        Collection<FieldMeta> fieldMetas = mainEntityMeta.getFieldMetas().values();
        Map<String, String> columnsValues = new LinkedHashMap<>();

        for (FieldMeta fieldMeta : fieldMetas) {
            Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
            Object getter = ReflectionUtil.invokeGetter(entity, fieldMeta.getFieldName());
            System.out.println("INVOKE GETTER RESULT OF: " + entity.getClass() + " : " + getter);
            if (!annotations.containsKey(Id.class) && annotations.containsKey(Column.class)) {
                columnsValues.put(fieldMeta.getColumnName(), String.valueOf(getter));
            } else if (annotations.containsKey(ManyToMany.class)) {
                System.out.println("contains many to many ");
                buildManyToManyJoinQuery(entity, fieldMeta, insertQueries, joinqueries, processedObjects);
            }
        }


        String columns = String.join(", ", columnsValues.keySet());
        String values = String.join(", ", columnsValues.values());


        String insertQuery = "insert into " +
                mainEntityMeta.getTableName() +
                " (" + columns +
                ") values (" +
                values + ")" +
                ";";

        insertQueries.add(insertQuery);
    }


    /**
     * Builds join query with
     * many to many association
     *
     * @param mainEntity
     * @param fieldMeta
     * @param insertQueries
     * @return
     */
    private static void buildManyToManyJoinQuery(final Object mainEntity,
                                                 final FieldMeta fieldMeta,
                                                 final List<String> insertQueries,
                                                 final List<String> joinQueries,
                                                 final Set<ProcessedObject> processedObjects) throws DefaultOrmException {


        ProcessedObject processedObject = new ProcessedObject(mainEntity.getClass(), fieldMeta.getFieldName());
        if (!processedObjects.contains(processedObject)) {
            processedObjects.add(processedObject);


            EntityMeta mainEntityMeta = CACHE_PROCESSOR.getMeta(mainEntity.getClass());
            Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
            ManyToMany manyToMany = (ManyToMany) annotations.get(ManyToMany.class);
            String mappedFieldName = manyToMany.mappedBy();
            CascadeType[] cascade = manyToMany.cascade();

            boolean contains = Arrays.asList(cascade).contains(CascadeType.SAVE_UPDATE);
            if (!contains) {
                return;
            }

            /** можно вынести в отдельный метод*/

            Class<?> fieldGenericType = fieldMeta.getFieldGenericType();
            EntityMeta joinEntityMeta = CACHE_PROCESSOR.getMeta(fieldGenericType);

            JoinTable joinTable = null;
            boolean reverse = false;


            if (mappedFieldName.isEmpty()) {
                joinTable = (JoinTable) annotations.get(JoinTable.class);
                reverse = true;

            } else {
                Collection<FieldMeta> joinEntityFieldMetas = joinEntityMeta.getFieldMetas().values();
                FieldMeta joinEntityFieldMeta = findFieldMetaByMappedFieldName(joinEntityFieldMetas, mappedFieldName);
                joinTable = (JoinTable) joinEntityFieldMeta.getAnnotations().get(JoinTable.class);
            }


            Object mainEntityId = ReflectionUtil.invokeGetter(mainEntity, mainEntityMeta.getIdColumnFieldName());
            Collection joinEntities = (Collection) ReflectionUtil.invokeGetter(mainEntity, fieldMeta.getFieldName());
            if (joinEntities != null && !joinEntities.isEmpty()) {
                for (Object entity : joinEntities) {
                    buildComplexInsertQuery(entity, insertQueries, joinQueries, processedObjects);
                    Object joinEntityId = ReflectionUtil.invokeGetter(entity, joinEntityMeta.getIdColumnFieldName());
                    String joinTableQuery = buildQueryWithJoinTable(mainEntityId, joinEntityId, joinTable, reverse);
                    System.out.println("adding join table query " + joinTableQuery);
                    joinQueries.add(joinTableQuery);
                }
            }
        }


    }


    /**
     * Builds correct query with join table;
     *
     * @param mainEntityId
     * @param joinEntityId
     * @param joinTable
     * @param reverse
     * @return
     */
    private static String buildQueryWithJoinTable(Object mainEntityId,
                                                  Object joinEntityId,
                                                  final JoinTable joinTable,
                                                  final boolean reverse) {


        String joinTableName = joinTable.name();
        String joinColumn = joinTable.joinColumns()[0].name();
        String inverseJoinColumn = joinTable.inverseJoinColumns()[0].name();

        if (!reverse) {
            String temp = joinColumn;
            joinColumn = inverseJoinColumn;
            inverseJoinColumn = temp;

            Object tempId = mainEntityId;
            mainEntityId = joinEntityId;
            joinEntityId = tempId;
        }

        return "insert into " + joinTableName +
                "(`" + joinColumn + "` , `" + inverseJoinColumn + "`)"
                + " values ('" + joinEntityId + "' , '" + mainEntityId + "')"
                + "on duplicate key update `"
                + inverseJoinColumn + "` values = (`" + inverseJoinColumn + "`);";

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
    private static FieldMeta findFieldMetaByMappedFieldName(final Collection<FieldMeta> fieldMetas, final String mappedFieldName) throws DefaultOrmException {
        return fieldMetas
                .stream()
                .filter(f -> f.getFieldName().equals(mappedFieldName)).findAny()
                .orElseThrow(() -> new DefaultOrmException("Mapped field: " + mappedFieldName + " not found"));
    }


}
