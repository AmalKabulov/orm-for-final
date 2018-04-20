package com.ititon.jdbc_orm.processor.database;

import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DSConnector {

    private static final DSConnector INSTANCE = new DSConnector();


    private DSConnector() {
    }


    public Transaction beginTransaction() throws DefaultOrmException {
        try {
            ReflectionUtil.newClass(DSProperties.DRIVER);
            Connection connection = DriverManager.getConnection(DSProperties.URL,
                                                                DSProperties.USERNAME,
                                                                DSProperties.PASSWORD);
            return new Transaction(connection);
        } catch (SQLException e) {
            throw new DefaultOrmException("Error while creating connection ", e);
        }

    }

    public static DSConnector getINSTANCE() {
        return INSTANCE;
    }
}
