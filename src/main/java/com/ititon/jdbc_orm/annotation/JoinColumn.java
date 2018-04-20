package com.ititon.jdbc_orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, ElementType.FIELD})
@Retention(RUNTIME)
public @interface JoinColumn {

    /**
     * The name of the foreign key column.
     * The table in which it is found depends upon the
     * context.
     */
    String name() default "";
}
