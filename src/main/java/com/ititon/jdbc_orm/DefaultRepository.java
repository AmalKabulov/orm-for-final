package com.ititon.jdbc_orm;

import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.processor.database.DefaultConnectionPool;
import com.ititon.jdbc_orm.processor.listener.EventListenerDirector;
import com.ititon.jdbc_orm.processor.listener.event.EventType;
import com.ititon.jdbc_orm.processor.listener.event.InsertEvent;
import com.ititon.jdbc_orm.processor.listener.event.SelectEvent;
import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.processor.listener.InsertEventListener;
import com.ititon.jdbc_orm.processor.listener.SelectEventListener;
import com.ititon.jdbc_orm.processor.parser.ResultSetParser;
import com.ititon.jdbc_orm.processor.listener.query_builder.GenericQuery;
import com.ititon.jdbc_orm.util.Assert;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DefaultRepository<E, ID extends Serializable> implements IDefaultRepository<E, ID> {
    private final CacheProcessor cacheProcessor = CacheProcessor.getInstance();
    private final DefaultConnectionPool connectionPool = DefaultConnectionPool.getInstance();
    private final EventListenerDirector eventListener = EventListenerDirector.getInstance();
//    private final SelectEventListener selectEventListener = new SelectEventListener();
//    private final InsertEventListener insertEventListener = new InsertEventListener();


    @SuppressWarnings("unchecked")
    @Override
    public List<E> findAll() throws DefaultOrmException {
        Class<E> entityClass = getParametrizeClass();
        List<Object> entitiesFromCache = cacheProcessor.getEntitiesByClass(entityClass);
        try (Connection connection = connectionPool.getConnection()){
            Long rowCounts = getRowCounts(entityClass, connection);
            if (rowCounts != null && rowCounts == entitiesFromCache.size()) {
                System.out.println("Returning from cache");
                return ((List<E>) entitiesFromCache);
            }

            eventListener.executeEvent(EventType.SELECT, new SelectEvent(connection, entityClass));
            return (List<E>) cacheProcessor.getEntitiesByClass(entityClass);
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while finding all.", e);
        }
    }


    @SuppressWarnings("unchecked")
    public List<E> findByLimit(final int skip, final int count) throws DefaultOrmException {
        Class<E> entityClass = getParametrizeClass();
        List<Object> entitiesFromCache = cacheProcessor.getEntitiesByClass(entityClass);

        if (entitiesFromCache.size() >= skip + count) {
            return (List<E>) entitiesFromCache.stream().skip(skip).limit(count);
        }

        try (Connection connection = connectionPool.getConnection()) {
            eventListener.executeEvent(EventType.SELECT, new SelectEvent(connection, entityClass, skip, count));
            System.out.println("ADDED TO CACHE AND RETURNING....");
            return (List<E>) cacheProcessor.getEntitiesByClass(entityClass)
                    .stream()
                    .skip(skip)
                    .limit(count);
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while finding by limit", e);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public E findOne(ID id) throws DefaultOrmException {
        Class<E> entityClass = getParametrizeClass();
        Object entityFromCache = cacheProcessor.getEntity(entityClass, id);
        if (entityFromCache != null) {
            System.out.println("FROM CACHE...");
            return (E) entityFromCache;
        }


        System.out.println("ID TYPE IS: " + id.getClass());
        try (Connection connection = connectionPool.getConnection()) {
            eventListener.executeEvent(EventType.SELECT, new SelectEvent(connection, entityClass, id));
            return (E) cacheProcessor.getEntity(entityClass, id);
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while searching by id. ", e);
        }

    }

    @Override
    public void delete(ID id) throws DefaultOrmException {
        Class<E> entityClass = getParametrizeClass();
        String query = GenericQuery.deleteQuery(entityClass, id);

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.executeUpdate();
            cacheProcessor.deleteEntity(entityClass, id);
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while deleting by id.", e);
        }

    }

    @Override
    public E save(E entity) throws DefaultOrmException {
        Connection connection = connectionPool.getConnection();

        try (Connection c = connection) {
            eventListener.executeEvent(EventType.INSERT_UPDATE,
                    new InsertEvent(connection, InsertEvent.Type.SAVE, entity));
            return entity;

        } catch (SQLException e) {
            throw new DefaultOrmException("Error while saving entity: " + entity, e);
        }
    }

    @Override
    public E update(E entity) throws DefaultOrmException {
        Connection connection = connectionPool.getConnection();

        try (Connection c = connection) {
            eventListener.executeEvent(EventType.INSERT_UPDATE,
                    new InsertEvent(connection, InsertEvent.Type.UPDATE, entity));
            return entity;

        } catch (SQLException e) {
            throw new DefaultOrmException("Error while updating entity: " + entity, e);
        }
    }

    private Long getRowCounts(final Class<E> clazz, Connection connection) throws DefaultOrmException {
        String countQuery = GenericQuery.buildCountQuery(clazz);
        Long rowCount = null;

        try (PreparedStatement preparedStatement = connection.prepareStatement(countQuery)) {
            ResultSet counts = preparedStatement.executeQuery();
            if (counts.next()) {
                rowCount = counts.getLong(1);
            }
            return rowCount;
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while getting row counts. ", e);
        }


    }


    @SuppressWarnings("unchecked")
    private Class<E> getParametrizeClass() {
        return (Class<E>) ReflectionUtil.getGenericParameterClass(getClass(), 0);
    }

}
