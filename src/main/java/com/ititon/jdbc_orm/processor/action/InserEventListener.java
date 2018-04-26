package com.ititon.jdbc_orm.processor.action;

import com.ititon.jdbc_orm.ProcessedObject;
import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.event.InsertEvent;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class InserEventListener {

    private final CacheProcessor cacheProcessor = CacheProcessor.getInstance();

    public void onInsert(final InsertEvent event) {
        Object entity = event.getEntity();
        EntityMeta entityMeta = cacheProcessor.getMeta(entity.getClass());
    }

    private void onInsert(final EntityMeta entityMeta) {


        Collection<FieldMeta> fieldMetas = entityMeta.getFieldMetas().values();
        Map<String, String> columnsValues = new LinkedHashMap<>();

        for (FieldMeta fieldMeta : fieldMetas) {
            Map<Class<? extends Annotation>, Annotation> annotations = fieldMeta.getAnnotations();
            Object result = ReflectionUtil.invokeGetter(entity, fieldMeta.getFieldName());
            if (annotations.containsKey(Id.class)) {

            } else if (annotations.containsKey(Column.class)) {
                //TODO may be nullPointer - string.valueOf()
                columnsValues.put(fieldMeta.getColumnName(), String.valueOf(result));
            } else if (annotations.containsKey(ManyToMany.class)) {

            } else if (annotations.containsKey(OneToMany.class)) {

            } else if (annotations.containsKey(ManyToOne.class)) {

            } else if (annotations.containsKey(OneToOne.class)) {

            }
        }

    }


    private void handleFieldWiithOneToManyAssociation(final Object mainEntity,
                                                      final FieldMeta fieldMeta,
                                                      final Set<ProcessedObject> processedObjects) {

        ProcessedObject processedObject = new ProcessedObject(mainEntity.getClass(), fieldMeta.getFieldName());
        if (!processedObjects.contains(processedObject)) {
            processedObjects.add(processedObject);

            OneToMany oneToMany = (OneToMany) fieldMeta.getAnnotations().get(OneToMany.class);
            CascadeType[] cascade = oneToMany.cascade();
            boolean contains = Arrays.asList(cascade).contains(CascadeType.SAVE_UPDATE);
            if (!contains) {
                return;
            }

            Class<?> fieldGenericType = fieldMeta.getFieldGenericType();
            EntityMeta joinEntity = cacheProcessor.getMeta(fieldGenericType);
            onInsert(joinEntity);
        }

    }

    public void handleFieldWithManyToManyAnnotation(final Object mainEntity,
                                                    final FieldMeta fieldMeta,
                                                    final Set<ProcessedObject> processedObjects) {


        ProcessedObject processedObject = new ProcessedObject(mainEntity.getClass(), fieldMeta.getFieldName());
        if (!processedObjects.contains(processedObject)) {
            processedObjects.add(processedObject);


            EntityMeta mainEntityMeta = cacheProcessor.getMeta(mainEntity.getClass());
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
            EntityMeta joinEntityMeta = cacheProcessor.getMeta(fieldGenericType);

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
//                    buildComplexInsertQuery(entity, insertQueries, joinQueries, processedObjects);
                    Object joinEntityId = ReflectionUtil.invokeGetter(entity, joinEntityMeta.getIdColumnFieldName());
//                    String joinTableQuery = buildQueryWithJoinTable(mainEntityId, joinEntityId, joinTable, reverse);
//                    System.out.println("adding join table query " + joinTableQuery);
//                    joinQueries.add(joinTableQuery);
                }
            }
        }
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


    private class FirstQueue {

    }

    private class SecondQueue {

    }

    private class ThirdQueue {

    }
}
