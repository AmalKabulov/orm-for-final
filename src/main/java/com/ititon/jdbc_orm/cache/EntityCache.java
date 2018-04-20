package com.ititon.jdbc_orm.cache;


import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *Cache for entities
 */
public class EntityCache/*<K extends BaseEntity>*/ /*implements Cache1<EntityCache.Key<ID, K>, K>*/ {
    /**
     *Object lifetime in cache
     */
    private final long timeToLive;

    private ConcurrentHashMap<Key, CacheObject> values;

    public EntityCache(final long timeToLive, final long idleInterval, final int maxItems) {
        values = new ConcurrentHashMap<>(maxItems);
        this.timeToLive = timeToLive;

        if (timeToLive > 0 && idleInterval > 0) {
            startDaemonThread(idleInterval);
        }
    }


    /**
     * Daemon thread which cleans the cache after a specified amount of time
     * @param idleInterval
     */
    private void startDaemonThread(final long idleInterval) {
        final Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(idleInterval);
                } catch (InterruptedException e) {
                    //TODO
                }
                cleanup();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * Retrieves objects from the cache by id and Class
     * if cache. For this, method generates a special key
     * and then searches in cache by this.key;
     * If there is no object in the cache return null;
     * @param id
     * @param clazz
     * @return Object if found else null;
     */
    @SuppressWarnings("unchecked")
    public /*<K extends BaseEntity> K*/ Object get(final Serializable id, final Class<?/*extends BaseEntity*/> clazz) {
        final Key key = new Key(id, clazz);
        CacheObject cacheObject = getCacheObject(key);
        if (cacheObject != null) {
            return /*(K)*/ cacheObject.entity;
        }
        return null;
    }

    /**
     * Inserts an object into the cache.
     * For this, this method creates a special key
     * which contains id of entity and entity class
     * and a special object 'CacheObject'
     * and then inserts entity.
     * @param entity
     */
    @SuppressWarnings("unchecked")
    public void put(final /*BaseEntity*/ Object entity) {
        final Key key = new Key(getPrimaryKey(entity), entity.getClass());
        this.values.put(key, new CacheObject(entity));
    }


    /**
     * Removes entity by id and entity class.
     * For this?, method also generates a special key
     * and then searches by this key in cache. If an entity is found,
     * removes this;
     * @param id
     * @param clazz
     */
    public void remove(final Serializable id, final Class<? /*extends BaseEntity*/> clazz) {
        final Key key = new Key(id, clazz);
        if (getCacheObject(key) != null) {
            this.values.remove(key);
        }
    }

    /**
     * Searches entities in cache by entity class.
     * Then collects it to List and return;
     * This method can work for a long time depending
     * on the number of objects in the cache, because it
     * retrieves all the items in cache.
     * @param clazz
     * @return List<Objects>
     */
    public List<Object/*? extends BaseEntity*/> getByClass(final Class<? /*extends BaseEntity*/> clazz) {
        List<Object> entities = new ArrayList<>();
        values.forEach((k, v) -> {
            if (k.getClass() == clazz) {
                entities.add(v.entity);
            }
        });

        return entities;
    }

    /**
     * This method cleans the cache.
     * Used by thread;
     */
    private void cleanup() {
        final long now = System.currentTimeMillis();
        final Iterator<Map.Entry<Key, CacheObject>> iterator = this.values.entrySet().iterator();
        CacheObject cacheObject = null;

        while (iterator.hasNext()) {
            cacheObject = iterator.next().getValue();

            if (cacheObject != null && (now > (this.timeToLive + cacheObject.createdTime))) {
                //TODO log
                System.out.println("removing from cache");
                iterator.remove();
            }
        }

    }


    /**
     * Searches CacheObject by special key;
     * @param key
     * @return CacheObject if not null;
     */
    private CacheObject getCacheObject(Key key) {
        final CacheObject cacheObject = this.values.get(key);
        if (cacheObject == null) {
            return null;
        } else if (this.timeToLive > 0 && System.currentTimeMillis() > this.timeToLive + cacheObject.createdTime) {
            this.values.remove(key);
            return null;
        }
        return cacheObject;
    }


    /**
     * Wrapper for entities class and fields;
     * For generating complex composite keys;
     */
    private class Key {
        private Class<?> clazz;
        private Serializable id;

        private Key(final Serializable id, final Class<?> clazz) {
            Objects.requireNonNull(id);
            Objects.requireNonNull(clazz);
            this.clazz = clazz;
            this.id = id;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equals(clazz, key.clazz) &&
                    Objects.equals(id, key.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, id);
        }
    }


    /**
     * Wrapper for entities;
     * Contains created time. By this.time thread deletes the object;
     */
    private class CacheObject {
        private final long createdTime = System.currentTimeMillis();
        private final /*BaseEntity*/ Object entity;

        private CacheObject(/*BaseEntity*/ Object entity) {
            this.entity = entity;
        }
    }


    /**
     * By Reflection calls getter of primary key of object;
     * @param entity
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private  <T extends Serializable> T getPrimaryKey(Object entity) {
        T id = null;
        EntityMeta meta = CacheProcessor.getInstance().getMeta(entity.getClass());
        if (meta != null) {
            id = (T) ReflectionUtil.invokeGetter(entity, meta.getIdColumnFieldName());
        }
        return id;
    }

}
