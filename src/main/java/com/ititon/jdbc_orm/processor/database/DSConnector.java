package com.ititon.jdbc_orm.processor.database;

import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connector for datasource;
 */
public class DSConnector {

    private static final DSConnector INSTANCE = new DSConnector();


    private DSConnector() {
    }

    /**
     * Creates connection to database
     * by URL, USERNAME and PASSWORD and return it;
     * @return ProxyConnection
     * @throws DefaultOrmException
     */
    public ProxyConnection getConnection() throws DefaultOrmException {
        try {
            ReflectionUtil.newClass(DSProperties.DRIVER);
            java.sql.Connection connection = DriverManager.getConnection(DSProperties.URL,
                                                                DSProperties.USERNAME,
                                                                DSProperties.PASSWORD);
            return new ProxyConnection(connection);
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while creating connection ", e);
        }

    }

    public static DSConnector getINSTANCE() {
        return INSTANCE;
    }
}
