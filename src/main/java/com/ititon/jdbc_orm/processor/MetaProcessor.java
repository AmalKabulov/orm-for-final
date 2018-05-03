package com.ititon.jdbc_orm.processor;


import com.ititon.jdbc_orm.annotation.*;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaProcessor {


    public static Map<Class<?>, EntityMeta> collectMeta(final String pkgName) {

        Map<Class<?>, EntityMeta> entitiesMeta = new HashMap<>();
        List<Class<?>> entities = AnnotationProcessor.getClassesByAnnotation(Entity.class, pkgName);

        for (Class<?> clazz : entities) {
            EntityMeta entityMeta = createEntityMeta(clazz);
            entitiesMeta.put(clazz, entityMeta);
        }
        return entitiesMeta;
    }


    private static EntityMeta createEntityMeta(final Class<?> clazz) {
        EntityMeta entityMeta = new EntityMeta();

        Table table = clazz.getAnnotation(Table.class);
        String tableName = table.name();
        entityMeta.setTableName(tableName);
        entityMeta.setEntityClassName(clazz.getName());

        Field[] classFields = clazz.getDeclaredFields();

        for (Field field : classFields) {
            if (field.isAnnotationPresent(Id.class)) {
                String columnName = null;
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    columnName = column.name();

                } else if (field.isAnnotationPresent(JoinColumn.class)) {
                    JoinColumn column = field.getAnnotation(JoinColumn.class);
                    columnName = column.name();
                }
                entityMeta.setIdColumnFieldName(field.getName());
                entityMeta.setIdColumnType(field.getType());
                entityMeta.setIdColumnName(columnName);
            }

            FieldMeta fieldMeta = createFieldMeta(field);
            if (fieldMeta != null) {
                entityMeta.getFieldMetas().put(fieldMeta.getFieldName(), fieldMeta);
            }
        }

        return entityMeta;

    }


    private static FieldMeta createFieldMeta(final Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) &&
                Modifier.isFinal(modifiers)) {
            return null;
        }


        FieldMeta fieldMeta = new FieldMeta();
        fieldMeta.setFieldName(field.getName());
        fieldMeta.setFieldType(field.getType());


        if (field.isAnnotationPresent(Column.class))

        {
            Column column = field.getAnnotation(Column.class);
            String columnName = column.name();
            fieldMeta.setColumnName(columnName);


        } else if (field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class))

        {
            fieldMeta.setFieldGenericType(ReflectionUtil.getFieldGenericType(field));
        }

        fillFieldMetaWithAnnotations(field, fieldMeta);

        return fieldMeta;
    }


    private static void fillFieldMetaWithAnnotations(final Field field, final FieldMeta fieldMeta) {
        Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
        if (fieldAnnotations != null && fieldAnnotations.length > 0) {

            for (Annotation annotation : fieldAnnotations) {
                fieldMeta.getAnnotations().put(annotation.annotationType(), annotation);
            }

        }
    }

}
