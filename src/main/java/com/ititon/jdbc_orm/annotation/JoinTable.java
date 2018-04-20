package com.ititon.jdbc_orm.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, ElementType.FIELD})
@Retention(RUNTIME)
public @interface JoinTable {

    /**  The catalog of the table.
     * <p> Defaults to the default catalog.
     */
    String name() default "";


    /**
     * The foreign key columns
     * of the join table which reference the
     * primary table of the entity that does
     * not own the association.
     */
    JoinColumn[] joinColumns() default {};

    /**
     * The foreign key columns
     * of the join table which reference the
     * primary table of the entity that does
     * not own the association.
     */
    JoinColumn[] inverseJoinColumns() default {};
}
