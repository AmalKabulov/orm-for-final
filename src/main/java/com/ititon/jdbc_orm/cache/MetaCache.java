package com.ititon.jdbc_orm.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cache for Entities MetaData, is filled once
 * at the time the application was started
 * and keeps its state unmodifiable
 * until the end of the application.
 * @param <K>
 * @param <V>
 */
public class MetaCache<K, V> {

    private Map<K, V> values;

    public MetaCache(Map<K, V> values) {
        Objects.requireNonNull(values);
        LinkedHashMap<K, V> newMap = new LinkedHashMap<>(values);
        this.values = Collections.unmodifiableMap(newMap);
    }

    /**
     * Searches in metacache by key;
     * @param key
     * @return Value
     */
    public V get(K key) {
        return values.get(key);
    }
}
