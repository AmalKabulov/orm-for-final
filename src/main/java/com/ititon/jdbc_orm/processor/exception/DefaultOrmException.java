package com.ititon.jdbc_orm.processor.exception;

public class DefaultOrmException extends Exception {
    public DefaultOrmException() {
        super();
    }

    public DefaultOrmException(String message) {
        super(message);
    }

    public DefaultOrmException(String message, Throwable cause) {
        super(message, cause);
    }

    public DefaultOrmException(Throwable cause) {
        super(cause);
    }

    protected DefaultOrmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
