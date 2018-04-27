package com.ititon.jdbc_orm.processor.event.info;

import com.ititon.jdbc_orm.ProcessedObject;
import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.meta.FieldMeta;

import java.util.Set;

public class EntityInfo {

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
