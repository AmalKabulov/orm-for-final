package com.ititon.jdbc_orm.processor.listener.info;

public class JoinTableInfo {
    private String joinTableName;
    private Object mainEntity;
    private String mainEntityIdColumnName;
    private Object innerEntity;
    private String innerEntityIdColumnName;


    public JoinTableInfo(String joinTableName,
                         Object mainEntity,
                         String mainEntityIdColumnName,
                         Object innerEntity,
                         String innerEntityIdColumnName) {
        this.joinTableName = joinTableName;
        this.mainEntity = mainEntity;
        this.mainEntityIdColumnName = mainEntityIdColumnName;
        this.innerEntity = innerEntity;
        this.innerEntityIdColumnName = innerEntityIdColumnName;
    }

    public JoinTableInfo() {
    }




    public String getJoinTableName() {
        return joinTableName;
    }

    public void setJoinTableName(String joinTableName) {
        this.joinTableName = joinTableName;
    }

    public Object getMainEntity() {
        return mainEntity;
    }

    public void setMainEntity(Object mainEntity) {
        this.mainEntity = mainEntity;
    }

    public String getMainEntityIdColumnName() {
        return mainEntityIdColumnName;
    }

    public void setMainEntityIdColumnName(String mainEntityIdColumnName) {
        this.mainEntityIdColumnName = mainEntityIdColumnName;
    }

    public Object getInnerEntity() {
        return innerEntity;
    }

    public void setInnerEntity(Object innerEntity) {
        this.innerEntity = innerEntity;
    }

    public String getInnerEntityIdColumnName() {
        return innerEntityIdColumnName;
    }

    public void setInnerEntityIdColumnName(String innerEntityIdColumnName) {
        this.innerEntityIdColumnName = innerEntityIdColumnName;
    }
}
