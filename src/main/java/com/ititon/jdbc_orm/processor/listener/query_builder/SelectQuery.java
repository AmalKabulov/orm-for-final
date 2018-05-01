package com.ititon.jdbc_orm.processor.listener.query_builder;

import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.Assert;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public abstract class SelectQuery {
    private static final CacheProcessor CACHE_PROCESSOR = CacheProcessor.getInstance();

    /**
     * Method for build find by limit query,
     * with many to many, many to one,
     * one to many and one to one associations.
     * @param entityClass
     * @param skip
     * @param count
     * @return string query
     */
    public static String buildSelectByLimitQuery(final Class<?> entityClass,
                                                 final int skip, final int count) throws DefaultOrmException {
        String findAllQuery = buildSelectQuery(entityClass);
        StringBuilder limitQuery = new StringBuilder(findAllQuery);
        limitQuery.setLength(limitQuery.length() - 1);

        return limitQuery.append(" limit ").append(skip).append(", ").append(count).append(";").toString();
    }

    /**
     * Method for build find by id query,
     * with many to many, many to one,
     * one to many and one to one associations.
     *
     * @param entityClass
     * @param id
     * @return string query
     */
    public static String buildSelectByIdQuery(final Class<?> entityClass, Object id) throws DefaultOrmException {
        String selectQuery = buildSelectQuery(entityClass);
        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(entityClass);
        StringBuilder byIdQuery = new StringBuilder(selectQuery);
        byIdQuery.setLength(byIdQuery.length() - 1);

        return byIdQuery.append(" where ")
                .append(entityMeta.getTableName()).append(".").append(entityMeta.getIdColumnName())
                .append(" = ")
                .append(id).append(";").toString();
    }


    /**
     * Method for build query for find row counts in database
     *
     * @param clazz
     * @return string query
     */
    public static String buildSelectRowCountQuery(final Class<?> clazz) {
        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(clazz);
        String idColumnName = entityMeta.getIdColumnName();
        String tableName = entityMeta.getTableName();

        return "select count(" +
                tableName + "." + idColumnName +
                ") from " +
                tableName +
                ";";
    }

    /**
     * Method for build find all query,
     * with many to many, many to one,
     * one to many and one to one associations.
     *
     * @param entityClass
     * @return string query
     */
    public static String buildSelectQuery(final Class<?> entityClass) throws DefaultOrmException {
        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(entityClass);
        Assert.notNull(entityMeta, "entity " + entityClass + " not found");
        Set<String> processedMetas = new HashSet<>();
        List<String> columns = new ArrayList<>();
        StringBuilder joinQueries = new StringBuilder();

        buildComplexJoinQuery(entityMeta, columns, processedMetas, joinQueries);
        String allColumns = String.join(", ", columns);

        return "select " + allColumns + " from " + entityMeta.getTableName() + joinQueries.toString() + ";";
    }

    /**
     * Builds join query if entity
     * class has complex associations.
     *
     * @param entityMeta
     * @param columns
     * @param joinQueries
     * @return
     */
    private static void buildComplexJoinQuery(final EntityMeta entityMeta,
                                              final List<String> columns,
                                              final Set<String> processedMetas,
                                              final StringBuilder joinQueries) throws DefaultOrmException {

        if (!processedMetas.contains(entityMeta.getEntityClassName())) {
            processedMetas.add(entityMeta.getEntityClassName());

            Collection<FieldMeta> fieldMetas = entityMeta.getFieldMetas().values();
            columns.add(getColumns(entityMeta));

            for (FieldMeta fieldMeta : fieldMetas) {
                Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
                String joinQuery = null;

                if (annotations.containsKey(ManyToMany.class)) {
                    joinQuery = buildManyToManyJoinQuery(entityMeta, fieldMeta, processedMetas);
                } else if (annotations.containsKey(ManyToOne.class)) {
                    joinQuery = buildManyToOneJoinQuery(entityMeta, fieldMeta, processedMetas);
                } else if (annotations.containsKey(OneToMany.class)) {
                    joinQuery = buildOneToManyJoinQuery(entityMeta, fieldMeta, processedMetas);
                } else if (annotations.containsKey(OneToOne.class)) {
                    joinQuery = buildOneToOneJoinQuery(entityMeta, fieldMeta, processedMetas);
                }

                if (joinQuery != null) {
                    EntityMeta joinEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
                    joinQueries.append(joinQuery);
                    buildComplexJoinQuery(joinEntityMeta, columns, processedMetas, joinQueries);

                }
            }
        }

    }


    /**
     * Builds join query with
     * many to many association.
     *
     * @return
     */
    private static String buildManyToManyJoinQuery(final EntityMeta mainEntity,
                                                   final FieldMeta fieldMeta,
                                                   final Set<String> processedMetas) throws DefaultOrmException {
        EntityMeta joinEntity = getEntityMetaByFieldMeta(fieldMeta);
        if (processedMetas.contains(joinEntity.getEntityClassName())) {
            return null;
        }
        Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
        ManyToMany manyToMany = (ManyToMany) annotations.get(ManyToMany.class);

        if (manyToMany.fetch().equals(FetchType.LAZY)) {
            return null;
        }

        JoinTable joinTable = null;
        boolean reverse = false;
        String mappedFieldName = manyToMany.mappedBy();


        Collection<FieldMeta> fieldMetas = joinEntity.getFieldMetas().values();


        if (mappedFieldName.isEmpty()) {
            joinTable = (JoinTable) annotations.get(JoinTable.class);
            reverse = true;
        } else {
            FieldMeta field = findFieldMetaByMappedFieldName(fieldMetas, mappedFieldName);
            joinTable = (JoinTable) field.getAnnotations().get(JoinTable.class);
        }

        return buildQueryWithJoinTable(mainEntity, joinEntity, joinTable, reverse);

    }

    /**
     * Builds join query with many to one association;
     *
     * @param mainEntity
     * @param fieldMeta
     * @return
     */
    private static String buildManyToOneJoinQuery(final EntityMeta mainEntity,
                                                  final FieldMeta fieldMeta,
                                                  final Set<String> processedMetas) {
        EntityMeta joinEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
        if (processedMetas.contains(joinEntityMeta.getEntityClassName())) {
            return null;
        }

        Map<Class<? extends Annotation>, Annotation> fieldMetaAnnotations = fieldMeta.getAnnotations();
        ManyToOne manyToOne = (ManyToOne) fieldMetaAnnotations.get(ManyToOne.class);

        if (manyToOne.fetch().equals(FetchType.LAZY)) {
            return null;
        }

        String mainTableName = mainEntity.getTableName();
        JoinColumn joinColumnAnnotation = (JoinColumn) fieldMetaAnnotations.get(JoinColumn.class);
        String mainTableIdColumn = mainTableName + "." + joinColumnAnnotation.name();


        String joinTableName = joinEntityMeta.getTableName();
        String joinTableIdColumn = joinTableName + "." + joinEntityMeta.getIdColumnName();

        return buildJoinQuery(joinTableName, joinTableIdColumn, mainTableIdColumn);
    }

    /**
     * Builds join query with one to many association;
     *
     * @param entityMeta
     * @param fieldMeta
     * @return
     */
    private static String buildOneToManyJoinQuery(final EntityMeta entityMeta,
                                                  final FieldMeta fieldMeta,
                                                  final Set<String> processedMetas) throws DefaultOrmException {

        EntityMeta innerEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
        if (processedMetas.contains(innerEntityMeta.getEntityClassName())) {
            return null;
        }

        OneToMany oneToMany = (OneToMany) fieldMeta.getAnnotations().get(OneToMany.class);
        String mappedFieldName = oneToMany.mappedBy();
        if (oneToMany.fetch().equals(FetchType.LAZY)) {
            return null;
        }


        Collection<FieldMeta> fieldMetas = innerEntityMeta.getFieldMetas().values();
        FieldMeta innerField = findFieldMetaByMappedFieldName(fieldMetas, mappedFieldName);

        JoinColumn joinColumnAnnotation = (JoinColumn) innerField.getAnnotations().get(JoinColumn.class);

        String joinTableName = innerEntityMeta.getTableName();
        String joinTableIdColumn = joinTableName + "." + joinColumnAnnotation.name();
        String mainTableIdColumn = entityMeta.getTableName() + "." + entityMeta.getIdColumnName();

        return buildJoinQuery(joinTableName, joinTableIdColumn, mainTableIdColumn);
    }


    /**
     * Builds join query with one to one association;
     *
     * @param entityMeta
     * @param fieldMeta
     * @return
     */
    private static String buildOneToOneJoinQuery(final EntityMeta entityMeta,
                                                 final FieldMeta fieldMeta,
                                                 final Set<String> processedMetas) throws DefaultOrmException {

        EntityMeta innerEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
        System.out.println(innerEntityMeta);
        if (processedMetas.contains(innerEntityMeta.getEntityClassName())) {
            return null;
        }


        Map<Class<? extends Annotation>, Annotation> fieldAnnotations = fieldMeta.getAnnotations();
        OneToOne oneToOne = (OneToOne) fieldAnnotations.get(OneToOne.class);
        if (oneToOne.fetch().equals(FetchType.LAZY)) {
            return null;
        }


        String mappedFieldName = oneToOne.mappedBy();
        String mainTableName = entityMeta.getTableName();
        String joinTableName = innerEntityMeta.getTableName();
        String mainTableIdColumn = null;
        JoinColumn joinColumn = null;
        String joinTableIdColumn = null;

        if (mappedFieldName.length() > 0) {
            Collection<FieldMeta> fieldValues = innerEntityMeta.getFieldMetas().values();
            FieldMeta field = findFieldMetaByMappedFieldName(fieldValues, mappedFieldName);

            joinColumn = (JoinColumn) field.getAnnotations().get(JoinColumn.class);
            mainTableIdColumn = mainTableName + "." + entityMeta.getIdColumnName();
            joinTableIdColumn = joinTableName + "." + joinColumn.name();
        } else {
            joinColumn = (JoinColumn) fieldAnnotations.get(JoinColumn.class);
            mainTableIdColumn = mainTableIdColumn + "." + joinColumn.name();
            joinTableIdColumn = joinTableName + "." + innerEntityMeta.getIdColumnName();

        }


        return buildJoinQuery(joinTableName, joinTableIdColumn, mainTableIdColumn);
    }


    /**
     * Builds correct query with join table;
     *
     * @param mainEntity
     * @param joinEntity
     * @param joinTable
     * @param reverse
     * @return
     */
    private static String buildQueryWithJoinTable(final EntityMeta mainEntity,
                                                  final EntityMeta joinEntity,
                                                  final JoinTable joinTable,
                                                  final boolean reverse) {


        String joinTableName = joinTable.name();
        String joinColumn = joinTableName + "." + joinTable.joinColumns()[0].name();
        String inverseJoinColumn = joinTableName + "." + joinTable.inverseJoinColumns()[0].name();

        if (reverse) {
            String temp = joinColumn;
            joinColumn = inverseJoinColumn;
            inverseJoinColumn = temp;
        }
        return " left join " + joinTableName +
                " on " + inverseJoinColumn + " = " + mainEntity.getTableName() + "." + mainEntity.getIdColumnName() +
                " left join " + joinEntity.getTableName() +
                " on " + joinEntity.getTableName() + "." + joinEntity.getIdColumnName() + " = " + joinColumn;

    }


    /**
     * Build simple join query, without join tables;
     *
     * @param joinTableName
     * @param joinTableIdColumn
     * @param mainTableIdColumn
     * @return
     */
    private static String buildJoinQuery(String joinTableName, String joinTableIdColumn, String mainTableIdColumn) {
        return " left join " + joinTableName +
                " on " +
                joinTableIdColumn +
                " = " +
                mainTableIdColumn;
    }


    /**
     * Gets column names from Entity metadata
     * Creates correct format of column;
     * Adds prefix - table name;
     *
     * @param entityMeta
     * @return
     */
    private static String getColumns(final EntityMeta entityMeta) {
        String tableName = entityMeta.getTableName();
        Collection<FieldMeta> fieldMetas = entityMeta.getFieldMetas().values();
        List<String> columnsList = fieldMetas
                .stream()
                .filter(field -> !Objects.equals(field.getColumnName(), null))
                .map(field -> tableName + "." + field.getColumnName())
                .collect(Collectors.toList());

        return String.join(", ", columnsList);

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

    /**
     * Finds entity meta by fieldMeta;
     *
     * @param fieldMeta
     * @return
     */
    private static EntityMeta getEntityMetaByFieldMeta(final FieldMeta fieldMeta) {
        Class<?> entityClass = fieldMeta.getFieldGenericType();
        if (entityClass == null) {
            entityClass = fieldMeta.getFieldType();
        }
        return CACHE_PROCESSOR.getMeta(entityClass);

    }
}
