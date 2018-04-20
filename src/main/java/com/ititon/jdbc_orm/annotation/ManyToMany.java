package com.ititon.jdbc_orm.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ManyToMany {

    /** (Optional) Whether the association should be lazily loaded or
     * must be eagerly fetched. The EAGER strategy is a requirement on
     * the persistence provider runtime that the associated entities
     * must be eagerly fetched.  The LAZY strategy is a hint to the
     * persistence provider runtime.
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * The field that owns the relationship. Required unless
     * the relationship is unidirectional.
     */
    String mappedBy() default "";

}
