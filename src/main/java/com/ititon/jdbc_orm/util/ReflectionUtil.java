package com.ititon.jdbc_orm.util;

import com.ititon.jdbc_orm.processor.exception.ReflectionException;

import java.lang.reflect.*;

public class ReflectionUtil {

    public static Object invokeMethod(Object object, Method method) {
        try {
            return method.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ReflectionException("COULD NOT INVOKE METHOD " + method.getName() + " CAUSE: ", e);
        }

    }


    public static Object invokeMethod(Object object, Method method, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(object, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ReflectionException("COULD NOT INVOKE METHOD " + method.getName() + " CAUSE: ", e);
        }

    }

    public static Method getMethod(Class objClass, String methodName) {
        try {
            return objClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new ReflectionException("METHOD: " + methodName + " NOT FOUND ", e);
        }
    }


    public static Method getMethod(Class objClass, String methodName, Class<?>... parametersType) {
        try {
            return objClass.getDeclaredMethod(methodName, parametersType);
        } catch (NoSuchMethodException e) {
            throw new ReflectionException("METHOD: " + methodName + " NOT FOUND ", e);
        }
    }


    public static Class<?> newClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException("CLASS: " + className + " NOT FOUND ", e);
        }
    }


    public static Type[] getFieldGenericTypes(Field field) {
        ParameterizedType genericReturnType = (ParameterizedType) field.getGenericType();
        return genericReturnType.getActualTypeArguments();
    }


    public static Object newInstance(String className) {
        try {
            return newClass(className).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ReflectionException("COULD NOT CREATE INSTANCE OF: " + className, e);
        }
    }


    public static Object invokeGetter(Object object, String fieldName) {
        Method getter = getMethod(object.getClass(), "get" + capitalizeFirstLetter(fieldName));
        return ReflectionUtil.invokeMethod(object, getter);
    }

    public static Object invokeSetter(Object object, String fieldName, /*Class<?> fieldType,*/ Object arg) {
        Method setter = getMethod(object.getClass(), "set" + capitalizeFirstLetter(fieldName), arg.getClass());
        setter.setAccessible(true);
        return ReflectionUtil.invokeMethod(object, setter, arg);
    }


    public static Class<?> getFieldGenericType(Field field) {
        String fieldGenericType = ReflectionUtil.getFieldGenericTypes(field)[0].getTypeName();
        return ReflectionUtil.newClass(fieldGenericType);
    }


    public static Class<?> getGenericParameterClass(Class actualClass, int parameterIndex) {
        return (Class<?>) ((ParameterizedType) actualClass.getGenericSuperclass()).getActualTypeArguments()[parameterIndex];
    }

    private static String capitalizeFirstLetter(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
