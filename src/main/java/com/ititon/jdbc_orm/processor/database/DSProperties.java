package com.ititon.jdbc_orm.processor.database;

public final class DSProperties {

    public static String URL;
    public static String DRIVER;
    public static String USERNAME;
    public static String PASSWORD;
    public static Integer MAX_POOL_SIZE;


    private DSProperties(String url, String driver, String userName, String password, Integer maxPoolSize) {
        URL = url;
        DRIVER = driver;
        USERNAME = userName;
        PASSWORD = password;
        MAX_POOL_SIZE = maxPoolSize;
    }

    public static void init(String url, String driver, String userName, String password, Integer maxPoolSize) {
        new DSProperties(url, driver, userName, password, maxPoolSize);
    }
}
