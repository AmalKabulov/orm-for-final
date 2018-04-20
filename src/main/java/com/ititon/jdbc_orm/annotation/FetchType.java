package com.ititon.jdbc_orm.annotation;

/**
 * Defines strategies for fetching data from the database.
 * The <code>EAGER</code> strategy is a requirement on the persistence
 * provider runtime that data must be eagerly fetched. The
 * <code>LAZY</code> strategy is a hint to the persistence provider
 * runtime that data should be fetched lazily when it is
 * first accessed.
 */
public enum FetchType {

    /** Defines that data can be lazily fetched. */
    LAZY,

    /** Defines that data must be eagerly fetched. */
    EAGER
}
