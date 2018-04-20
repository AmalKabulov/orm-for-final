package com.ititon.jdbc_orm.processor.database;

public final class DSProperties {

    public static String URL;
    public static String DRIVER;
    public static String USERNAME;
    public static String PASSWORD;
    public static Integer MAX_POOL_SIZE;


//    static {
//       URL = DSResourceManager.getValue("db.url");
//       DRIVER = DSResourceManager.getValue("db.driver");
//       USERNAME = DSResourceManager.getValue("db.username");
//       PASSWORD = DSResourceManager.getValue("db.password");
//       MAX_POOL_SIZE = Integer.valueOf(DSResourceManager.getValue("db.con.size"));
//    }

    private DSProperties(String url, String driver, String userName, String password, Integer maxPoolSize) {
        URL = url;
        DRIVER = driver;
        USERNAME = userName;
        PASSWORD = password;
        MAX_POOL_SIZE = maxPoolSize;
    }

    public void init(String url, String driver, String userName, String password, Integer maxPoolSize) {
        new DSProperties(url, driver, userName, password, maxPoolSize);
    }
}
