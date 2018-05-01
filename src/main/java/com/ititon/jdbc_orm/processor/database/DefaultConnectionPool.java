package com.ititon.jdbc_orm.processor.database;


import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Something like connection pool;
 * Contains thread save collections of
 * free connections and used connections
 */
public class DefaultConnectionPool {
    private static DefaultConnectionPool instance;

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private ArrayBlockingQueue<Connection> free;
    private ArrayBlockingQueue<Connection> inUse;
    private DSConnector connector = DSConnector.getINSTANCE();
    private int poolSize;


    private DefaultConnectionPool() {
        poolSize = DSProperties.MAX_POOL_SIZE;
        free = new ArrayBlockingQueue<>(poolSize);
        inUse = new ArrayBlockingQueue<>(poolSize);
    }

    /**
     * In DefaultConnectionPool not instantiated
     * creates new instance of this,
     * else returns already created instance
     * @return DefaultConnectionPool
     */
    public static DefaultConnectionPool getInstance() {
        if (instance == null) {
            synchronized (DefaultConnectionPool.class) {
                if (instance == null) {
                    instance = new DefaultConnectionPool();
                }
            }
        }
        return instance;
    }


    /**
     * Initializes collection of
     * free connections;
     * @throws DefaultOrmException
     */
    public void init() throws DefaultOrmException {

        if (!initialized.get()) {
            for (int i = 0; i < poolSize; i++) {
                free.offer(connector.getConnection());
            }
            initialized.set(true);
        }

    }


    /**
     * Retrieves free connection from collection of free connections
     * and removes the head of this queue,
     * inserts it into collection of used connections
     * waiting if necessary until an element becomes available.
     * @return
     * @throws DefaultOrmException
     */
    public Connection getConnection() throws DefaultOrmException {

        if (!initialized.get()) {
            throw new DefaultOrmException("ProxyConnection pool not initialized");
        }
        try {
            Connection connection = free.take();
            inUse.add(connection);
            return connection;
        } catch (InterruptedException e) {
            throw new DefaultOrmException("Could not get connection");
        }
    }


    /**
     * Commits and closes connections;
     * @param queue
     * @throws DefaultOrmException
     */
    private void closeConnections(ArrayBlockingQueue<Connection> queue) throws DefaultOrmException {
        if (initialized.get()) {
            Connection connection;
            while ((connection = queue.poll()) != null) {
                try {
                    if (!connection.getAutoCommit()){
                        connection.commit();
                    }

                    ((ProxyConnection) connection).closeDown();
                } catch (SQLException e) {
                    throw new DefaultOrmException("Could not close connection");
                }
            }
        }
    }

    /**
     * Method loses all connections
     * @throws DefaultOrmException
     */
    public void destroy() throws DefaultOrmException {
        if (initialized.get()) {
            closeConnections(free);
            closeConnections(inUse);

            initialized.set(false);
        }

    }


    /**
     * Method transfers connection from state in use to state free
     * @param connection
     * @throws DefaultOrmException
     */
    public void freeConnection(Connection connection) throws DefaultOrmException {
        try {
            if (connection.isClosed()) {
                throw new DefaultOrmException("ProxyConnection is already closed. This is incorrect listener");
            }

            if (!connection.getAutoCommit()) {
                connection.setAutoCommit(true);
            }

            if (connection.isReadOnly()) {
                connection.setReadOnly(false);
            }

            if (inUse.contains(connection)) {
                if (!inUse.remove(connection)) {
                    throw new DefaultOrmException("Could not remove connection from using connection pool.");
                }
            }

            if (!free.offer(connection)) {
                throw new DefaultOrmException("Could not return connection to free connection pool.");
            }

        } catch (SQLException e) {
            throw new DefaultOrmException("Could not free connection: ", e);
        }
    }


}
