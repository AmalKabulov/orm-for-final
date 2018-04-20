package com.ititon.jdbc_orm.util;

import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public abstract class Assert {

    /**
     * Assert that an object is {@code null}.
     * <pre class="code">Assert.isNull(value, "The value must be null");</pre>
     * @param object the object to check
     * @param message the exception message to use if the assertion fails
     * @throws DefaultOrmException if the object is not {@code null}
     */
    public static void notNull(Object object, String message) throws DefaultOrmException {
        if (object == null) {
            throw new DefaultOrmException(message);
        }
    }


    /**
     * Assert that a collection contains elements; that is, it must not be
     * {@code null} and must contain at least one element.
     * <pre class="code">Assert.notEmpty(collection, "Collection must contain elements");</pre>
     * @param collection the collection to check
     * @param message the exception message to use if the assertion fails
     * @throws DefaultOrmException if the collection is {@code null} or
     * contains no elements
     */
    public static void notEmpty(Collection<?> collection, String message) throws DefaultOrmException {
        if (collection == null || collection.isEmpty()) {
            throw new DefaultOrmException(message);
        }
    }


    /**
     * Assert that a Map contains entries; that is, it must not be {@code null}
     * and must contain at least one entry.
     * <pre class="code">Assert.notEmpty(map, "Map must contain entries");</pre>
     * @param map the map to check
     * @param message the exception message to use if the assertion fails
     * @throws DefaultOrmException if the map is {@code null} or contains no entries
     */
    public static void notEmpty(Map<?, ?> map, String message) throws DefaultOrmException {
        if (map == null || map.isEmpty()) {
            throw new DefaultOrmException(message);
        }
    }


    public static void notZero(Object object, String message) throws DefaultOrmException {
        if (Objects.equals(object, null) || Objects.equals(object, 0)) {
            throw new DefaultOrmException(message);
        }
    }


    /**
     * Assert a boolean expression, throwing an {@code IllegalArgumentException}
     * if the expression evaluates to {@code false}.
     * <pre class="code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
     * @param expression a boolean expression
     * @param message the exception message to use if the assertion fails
     * @throws DefaultOrmException if {@code expression} is {@code false}
     */
    public static void isTrue(boolean expression, String message) throws DefaultOrmException {
        if (!expression) {
            throw new DefaultOrmException(message);
        }
    }





}
