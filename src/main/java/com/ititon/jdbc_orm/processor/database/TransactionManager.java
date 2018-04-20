package com.ititon.jdbc_orm.processor.database;


import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransactionManager {
    private static TransactionManager instance;

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private ArrayBlockingQueue<Connection> free;
    private ArrayBlockingQueue<Connection> inUse;
    private DSConnector connector = DSConnector.getINSTANCE();
    private int poolSize;


    private TransactionManager() {
        poolSize = DSProperties.MAX_POOL_SIZE;
        free = new ArrayBlockingQueue<>(poolSize);
        inUse = new ArrayBlockingQueue<>(poolSize);
    }

    public static TransactionManager getInstance() {
        if (instance == null) {
            synchronized (TransactionManager.class) {
                if (instance == null) {
                    instance = new TransactionManager();
                }
            }
        }
        return instance;
    }


    public void init() throws DefaultOrmException {

        if (!initialized.get()) {
            for (int i = 0; i < poolSize; i++) {
                free.offer(connector.beginTransaction());
            }
            initialized.set(true);
        }

    }

    public Connection getConnection() throws DefaultOrmException {

        if (!initialized.get()) {
            throw new DefaultOrmException("Connection pool not initialized");
        }
        try {
            Connection connection = free.take();
            inUse.add(connection);
            return connection;
        } catch (InterruptedException e) {
            throw new DefaultOrmException("Could not get connection");
        }
    }





    private void closeConnections(ArrayBlockingQueue<Connection> queue) throws DefaultOrmException {
        if (initialized.get()) {
            Connection connection;
            while ((connection = queue.poll()) != null) {
                try {
                    if (!connection.getAutoCommit()) ;
                    {
                        connection.commit();
                    }

                    ((Transaction) connection).closeDown();
                } catch (SQLException e) {
                    throw new DefaultOrmException("Could not close connection");
                }
            }
        }
    }

    public void destroy() throws DefaultOrmException {
        if (initialized.get()) {
            closeConnections(free);
            closeConnections(inUse);

            initialized.set(false);
        }

    }


    public void freeConnection(Connection connection) throws DefaultOrmException {
        try {
            if (connection.isClosed()) {
                throw new DefaultOrmException("Connection is already closed. This is incorrect action");
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
