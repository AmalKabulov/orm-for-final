package com.ititon.jdbc_orm.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MetaCache<K, V> {

    private Map<K, V> values;

    public MetaCache(Map<K, V> values) {
        Objects.requireNonNull(values);
        LinkedHashMap newMap = new LinkedHashMap<>(values);
        this.values = Collections.unmodifiableMap(newMap);
    }


    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V get(K key) {
        return values.get(key);
    }
}
