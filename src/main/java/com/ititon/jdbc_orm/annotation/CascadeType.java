package com.ititon.jdbc_orm.annotation;

/**
 * Defines the set of cascadable operations that are propagated
 * to the associated entity.
 * The value <code>cascade=ALL</code> is equivalent to
 * <code>cascade={PERSIST, MERGE, REMOVE, REFRESH, DETACH}</code>.
 *
 * @since jdbcorm 1.0
 */
public enum CascadeType {

    /** Cascade all operations */
    ALL,

    /** Cascade save and update operations */
    SAVE_UPDATE,

    /** Cascade remove operations */
    REMOVE
}
