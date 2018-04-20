package com.ititon.jdbc_orm.processor.database;

import java.util.ResourceBundle;

public class DSResourceManager {

    private static final ResourceBundle resource = ResourceBundle.getBundle("database");


    public DSResourceManager() {
    }


    public static String getValue(final String key) {

        return resource.getString(key);
    }
}
