package com.ititon.jdbc_orm;

import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;

import java.io.Serializable;
import java.util.List;

public interface IDefaultRepository<E, ID extends Serializable> {

    List<E> findAll() throws DefaultOrmException;

    E findOne(ID id) throws DefaultOrmException;

    void delete(ID id) throws DefaultOrmException;

    E save(E entity) throws DefaultOrmException;

    E update(E entity) throws DefaultOrmException;
}
