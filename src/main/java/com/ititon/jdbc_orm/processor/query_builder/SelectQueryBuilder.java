package com.ititon.jdbc_orm.processor.query_builder;

import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.Assert;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class SelectQueryBuilder extends QueryBuilder {
    private static final CacheProcessor CACHE_PROCESSOR = CacheProcessor.getInstance();

    public static String findAllQuery(final Class<? /*extends BaseEntity*/> clazz) throws DefaultOrmException {

        EntityMeta entityMeta = CACHE_PROCESSOR.getMeta(clazz);
        Assert.notNull(entityMeta, "entity " + clazz + " not found");
        Collection<FieldMeta> fieldMetas = entityMeta.getFieldMetas().values();
        List<String> columns = new ArrayList<>();
        columns.add(getColumns(entityMeta));
        StringBuilder joinQueries = new StringBuilder();
        StringBuilder selectQuery = new StringBuilder();


        for (FieldMeta fieldMeta : fieldMetas) {
            Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
            if (annotations.containsKey(ManyToMany.class)) {
                String manyToManyQuery = ComplexAssocHandler.buildManyToManyJoinQuery(entityMeta, fieldMeta);
                addToJoinQueries(joinQueries, manyToManyQuery);
                EntityMeta manyToManyEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
                columns.add(getColumns(manyToManyEntityMeta));
            } else if (annotations.containsKey(ManyToOne.class)) {
                String manyToOneQuery = ComplexAssocHandler.buildManyToOneJoinQuery(entityMeta, fieldMeta);
                addToJoinQueries(joinQueries, manyToOneQuery);
                EntityMeta manyToOneEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
                columns.add(getColumns(manyToOneEntityMeta));
            } else if (annotations.containsKey(OneToMany.class)) {
                String oneToManyQuery = ComplexAssocHandler.buildOneToManyJoinQuery(entityMeta, fieldMeta);
                addToJoinQueries(joinQueries, oneToManyQuery);
                EntityMeta oneToManyEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
                columns.add(getColumns(oneToManyEntityMeta));
            } else if (annotations.containsKey(OneToOne.class)) {
                String oneToOneQuery = ComplexAssocHandler.buildOneToOneJoinQuery(entityMeta, fieldMeta);
                addToJoinQueries(joinQueries, oneToOneQuery);
                EntityMeta oneToOneEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
                columns.add(getColumns(oneToOneEntityMeta));
            }
        }


        String allColumns = String.join(", ", columns);
        return selectQuery.append("select ")
                .append(allColumns)
                .append(" from ")
                .append(entityMeta.getTableName())
                .append(joinQueries).append(";").toString();

    }


    private static class ComplexAssocHandler {

        private static String buildManyToManyJoinQuery(final EntityMeta entityMeta, final FieldMeta fieldMeta) {
            Map<Class<? extends Annotation>, Annotation> fieldAnnotations = fieldMeta.getAnnotations();
            ManyToMany manyToMany = (ManyToMany) fieldAnnotations.get(ManyToMany.class);
            EntityMeta innerEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
            JoinTable joinTable = null;
            boolean reverse = false;

            if (manyToMany.fetch().equals(FetchType.LAZY)) {
                return null;
            }

            String mappedBy = manyToMany.mappedBy();

            if (mappedBy.length() > 0) {
                Collection<FieldMeta> fieldValues = innerEntityMeta.getFieldMetas().values();
                FieldMeta field = fieldValues
                        .stream()
                        .filter(f -> f.getFieldName().equals(mappedBy)).findAny()
                        .orElseThrow(() -> new IllegalStateException("Mapped field: " + mappedBy + " not found"));

                joinTable = (JoinTable) field.getAnnotations().get(JoinTable.class);

            } else {
                joinTable = (JoinTable) fieldAnnotations.get(JoinTable.class);
                reverse = true;
            }

            return buildQueryWithJoinTable(entityMeta, innerEntityMeta, joinTable, reverse);
        }


        private static String buildQueryWithJoinTable(EntityMeta metaWithMappedBy,
                                                      EntityMeta metaWithJoinTable,
                                                      JoinTable joinTable,
                                                      boolean reverse) {




            String joinTableName = joinTable.name();
            String joinColumn = joinTableName + "." + joinTable.joinColumns()[0].name();
            String inverseJoinColumn = joinTableName + "." + joinTable.inverseJoinColumns()[0].name();

            if (reverse) {
                String temp = joinColumn;
                joinColumn = inverseJoinColumn;
                inverseJoinColumn = temp;
            }
            return " left join " + joinTableName +
                    " on " + inverseJoinColumn + " = " + metaWithMappedBy.getTableName()+ "." + metaWithMappedBy.getIdColumnName() +
                    " left join " + metaWithJoinTable.getTableName() +
                    " on " + metaWithJoinTable.getTableName() + "." + metaWithJoinTable.getIdColumnName() + " = " + joinColumn;


        }


        private static String buildManyToOneJoinQuery(final EntityMeta entityMeta, final FieldMeta fieldMeta) {
            Map<Class<? extends Annotation>, Annotation> fieldMetaAnnotations = fieldMeta.getAnnotations();
            ManyToOne manyToOne = (ManyToOne) fieldMetaAnnotations.get(ManyToOne.class);

            if (manyToOne.fetch().equals(FetchType.LAZY)) {
                return null;
            }

            String mainTableName = entityMeta.getTableName();
            JoinColumn joinColumnAnnotation = (JoinColumn) fieldMetaAnnotations.get(JoinColumn.class);
            String mainTableIdColumn = mainTableName + "." + joinColumnAnnotation.name();

            EntityMeta innerEntityMeta = getEntityMetaByFieldMeta(fieldMeta);
            String joinTableName = innerEntityMeta.getTableName();
            String joinTableIdColumn = joinTableName + "." + innerEntityMeta.getIdColumnName();

            return buildJoinQuery(joinTableName, joinTableIdColumn, mainTableIdColumn);

        }


        private static String buildOneToManyJoinQuery(final EntityMeta entityMeta, final FieldMeta fieldMeta) {
            OneToMany oneToMany = (OneToMany) fieldMeta.getAnnotations().get(OneToMany.class);
            String mappedBy = oneToMany.mappedBy();
            if (oneToMany.fetch().equals(FetchType.LAZY)) {
                return null;
            }

            EntityMeta innerEntity = getEntityMetaByFieldMeta(fieldMeta);
            Collection<FieldMeta> fieldMetas = innerEntity.getFieldMetas().values();
            FieldMeta innerField = fieldMetas.stream()
                    .filter(field -> field.getFieldName().equals(mappedBy))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("Mapped field: " + mappedBy + " not found"));

            JoinColumn joinColumnAnnotation = (JoinColumn) innerField.getAnnotations().get(JoinColumn.class);

            String joinTableName = innerEntity.getTableName();
            String joinTableIdColumn = joinTableName + "." + joinColumnAnnotation.name();
            String mainTableIdColumn = entityMeta.getIdColumnName();

            return buildJoinQuery(joinTableName, joinTableIdColumn, mainTableIdColumn);
        }



        private static String buildOneToOneJoinQuery(final EntityMeta entityMeta, final FieldMeta fieldMeta) {
            Map<Class<? extends Annotation>, Annotation> fieldAnnotations = fieldMeta.getAnnotations();
            OneToOne oneToOne = (OneToOne) fieldAnnotations.get(OneToOne.class);
            EntityMeta innerEntityMeta = getEntityMetaByFieldMeta(fieldMeta);

            if (oneToOne.fetch().equals(FetchType.LAZY)) {
                return null;
            }


            String mappedBy = oneToOne.mappedBy();
            String mainTableName = entityMeta.getTableName();
            String joinTableName = innerEntityMeta.getTableName();
            String mainTableIdColumn = null;
            JoinColumn joinColumn = null;
            String joinTableIdColumn = null;

            if (mappedBy.length() > 0) {
                Collection<FieldMeta> fieldValues = innerEntityMeta.getFieldMetas().values();
                FieldMeta field = fieldValues
                        .stream()
                        .filter(f -> f.getFieldName().equals(mappedBy)).findAny()
                        .orElseThrow(() -> new IllegalStateException("Mapped field: " + mappedBy + " not found"));

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


        private static String buildJoinQuery(String joinTableName, String joinTableIdColumn, String mainTableIdColumn) {
            return " left join " + joinTableName +
                    " on " +
                    joinTableIdColumn +
                    " = " +
                    mainTableIdColumn;
        }

        private static FieldMeta findFieldMetaByMappedByValue() {
            return null;
        }


    }


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

    private static EntityMeta getEntityMetaByFieldMeta(final FieldMeta fieldMeta) {
        Class<?> fieldGenericType = fieldMeta.getFieldGenericType();
        return CACHE_PROCESSOR.getMeta(fieldGenericType);

    }

    private static void addToJoinQueries(final StringBuilder joinQueries, String joinQuery) {
        if (joinQuery != null && joinQuery.length() > 0) {
            joinQueries.append(joinQuery);
        }

    }
}
