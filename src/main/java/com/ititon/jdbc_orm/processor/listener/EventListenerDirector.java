package com.ititon.jdbc_orm.processor.listener;

import com.ititon.jdbc_orm.processor.exception.DefaultOrmException;
import com.ititon.jdbc_orm.processor.listener.event.Event;
import com.ititon.jdbc_orm.processor.listener.event.EventType;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EventListenerDirector {
    private static final EventListenerDirector INSTANCE = new EventListenerDirector();

    private final Map<EventType, EventListener> events = new HashMap<>();

    private EventListenerDirector() {
        events.put(EventType.INSERT_UPDATE, new InsertEventListener());
        events.putIfAbsent(EventType.SELECT, new SelectEventListener());
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> void executeEvent(EventType eventType, E event) throws SQLException, DefaultOrmException {
        events.get(eventType).execute(event);
    }

    public static EventListenerDirector getInstance() {
        return INSTANCE;
    }
}
