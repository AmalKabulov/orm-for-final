package com.ititon.jdbc_orm.processor.listener;

import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.processor.listener.event.Event;
import com.ititon.jdbc_orm.processor.listener.event.InsertEvent;

import java.sql.SQLException;

public interface EventListener<E extends Event> {

    void execute(E event) throws SQLException, DefaultOrmException;
}
