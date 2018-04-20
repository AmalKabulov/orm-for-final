package com.ititon.jdbc_orm.util;

public class ClassUtil {

    public static StackTraceElement getCallerMethodInfo() {

        return Thread.currentThread().getStackTrace()[3];
    }
}
