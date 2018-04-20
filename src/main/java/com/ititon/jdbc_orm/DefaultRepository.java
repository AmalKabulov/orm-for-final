package com.ititon.jdbc_orm;

import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.database.TransactionManager;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.processor.parser.Parser;
import com.ititon.jdbc_orm.processor.query_builder.QueryBuilder;
import com.ititon.jdbc_orm.processor.query_builder.SelectQueryBuilder;
import com.ititon.jdbc_orm.util.Assert;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DefaultRepository<E, ID extends Serializable> implements IDefaultRepository<E, ID> {
    private CacheProcessor cacheProcessor = CacheProcessor.getInstance();
    private TransactionManager connectionPool = TransactionManager.getInstance();


    @Override
    public List<E> findAll() throws DefaultOrmException {
        List<E> entities = null;
        Class<E/*extends BaseEntity*/> entityClass = getParametrizeClass();
        String query = SelectQueryBuilder.findAllQuery(entityClass);
        System.out.println("QUERY: " + query);

        List<Object> entitiesFromCache = cacheProcessor.getEntitiesByClass(entityClass);


        try (Connection connection = connectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            Long rowCounts = getRowCounts(entityClass, connection);

            if (rowCounts != null && rowCounts == entitiesFromCache.size()) {
                System.out.println("Returning from cache");
                return ((List<E>) entitiesFromCache);
            }

            Parser parser = new Parser();
            ResultSet resultSet = preparedStatement.executeQuery();
//            parserManager.complexParse(entityClass, resultSet);
            entities = (List<E>) parser.parseComplex(entityClass, resultSet);

            Assert.notEmpty(entities, "Nothing was found");
            return entities;
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while finding all.", e);
        }
    }

    public List<E> findByLimit(final int skip, final int count) throws DefaultOrmException {
        List<E> entities = new ArrayList<>();
        Class<E/*extends BaseEntity*/> entityClass = getParametrizeClass();
        String query = QueryBuilder.findByLimit(entityClass, skip, count);

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            Parser parser = new Parser();
            while (resultSet.next()) {
                Object entity = parser.parseSimple(entityClass, resultSet);
                entities.add((E) entity);
            }

            Assert.notEmpty(entities, "Nothing was found");
            cacheProcessor.putAll(entities);
            return entities;

        } catch (SQLException e) {
            throw new DefaultOrmException("Error while finding by limit", e);
        }

    }

    @Override
    public E findOne(ID id) throws DefaultOrmException {
        Class<E/*extends BaseEntity*/> entityClass = getParametrizeClass();
        Object entityFromCache = cacheProcessor.getEntity(entityClass, id);
        if (entityFromCache != null) {
            System.out.println("FROM CACHE...");
            return (E) entityFromCache;
        }

        Object entity = null;
        String query = QueryBuilder.findByIdQuery(entityClass, id);
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            ResultSet resultSet = preparedStatement.executeQuery();
            Parser parser = new Parser();
            while (resultSet.next()) {
                entity = parser.parseSimple(entityClass, resultSet);
            }
            Assert.notNull(entity, "Nothing was found");
            cacheProcessor.putEntity(entity);
            return (E) entity;
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while searching by id. ", e);
        }

    }

    @Override
    public void delete(ID id) throws DefaultOrmException {
        Class<E/*extends BaseEntity*/> entityClass = getParametrizeClass();
        String query = QueryBuilder.deleteQuery(entityClass, id);

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int result = preparedStatement.executeUpdate();
            Assert.notZero(result, "Deleting entity: " + entityClass + " failed");
            cacheProcessor.deleteEntity(entityClass, id);
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while deleting by id.", e);
        }

    }

    @Override
    public E save(E entity) throws DefaultOrmException {
        String query = QueryBuilder.insertQuery(entity);
        EntityMeta entityMeta = cacheProcessor.getMeta(entity.getClass());
        Assert.notNull(entityMeta, "entity: " + entity + " not found");
        String idColumnFieldName = entityMeta.getIdColumnFieldName();

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int result = preparedStatement.executeUpdate();
            Assert.notZero(result, "inserting entity: " + entity + " failed");
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            Assert.isTrue(resultSet.next(), "inserting entity: " + entity + " failed. No id obtained");
            //TODO может быть не лонг
            Long id = resultSet.getLong(1);
            ReflectionUtil.invokeSetter(entity, idColumnFieldName, id);

//            cacheProcessor.putEntity(entity);
            return entity;

        } catch (SQLException e) {
            throw new DefaultOrmException("Error while saving entity: " + entity, e);
        }
    }

    @Override
    public E update(E entity) throws DefaultOrmException {
        String query = QueryBuilder.updateQuery(entity);
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            int updatedRows = preparedStatement.executeUpdate();
            Assert.notZero(updatedRows, "updating entity: " + entity + " failed");
//            cacheProcessor.putEntity(entity);
            return entity;

        } catch (SQLException e) {

            throw new DefaultOrmException("Error while updating entity: " + entity, e);
        }
    }

    private Long getRowCounts(final Class<? /*extends BaseEntity*/> clazz, Connection connection) throws SQLException, DefaultOrmException {
        String countQuery = QueryBuilder.countQuery(clazz);
        Long rowCount = null;

        PreparedStatement preparedStatement = connection.prepareStatement(countQuery);
        ResultSet counts = preparedStatement.executeQuery();
        if (counts.next()) {
            rowCount = counts.getLong(1);
        }
        preparedStatement.close();
        return rowCount;


    }


    @SuppressWarnings("unchecked")
    private Class<E /*extends BaseEntity*/> getParametrizeClass() {
        return /*(Class<? extends BaseEntity>)*/ (Class<E>) ReflectionUtil.getGenericParameterClass(getClass(), 0);
    }

}
