package com.ititon.jdbc_orm.cache;


import com.ititon.jdbc_orm.meta.EntityMeta;
import com.ititon.jdbc_orm.processor.CacheProcessor;
import com.ititon.jdbc_orm.util.ReflectionUtil;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityCache/*<K extends BaseEntity>*/ /*implements Cache1<EntityCache.Key<ID, K>, K>*/ {

    private final long timeToLive;
    private ConcurrentHashMap<Key, CacheObject> values;

    public EntityCache(final long timeToLive, final long idleInterval, final int maxItems) {
        values = new ConcurrentHashMap<>(maxItems);
        this.timeToLive = timeToLive;

        if (timeToLive > 0 && idleInterval > 0) {
            startDaemonThread(idleInterval);
        }
    }

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



    @SuppressWarnings("unchecked")
    public /*<K extends BaseEntity> K*/ Object get(final Serializable id, final Class<?/*extends BaseEntity*/> clazz) {
        final Key key = new Key(id, clazz);
        CacheObject cacheObject = getCacheObject(key);
        if (cacheObject != null) {
            return /*(K)*/ cacheObject.entity;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void put(final /*BaseEntity*/ Object entity) {
        final Key key = new Key(getPrimaryKey(entity), entity.getClass());
        this.values.put(key, new CacheObject(entity));
    }


    public void remove(final Serializable id, final Class<? /*extends BaseEntity*/> clazz) {
        final Key key = new Key(id, clazz);
        if (getCacheObject(key) != null) {
            this.values.remove(key);
        }
    }


    public List<Object/*? extends BaseEntity*/> getByClass(final Class<? /*extends BaseEntity*/> clazz) {
        List<Object> entities = new ArrayList<>();
        values.forEach((k, v) -> {
            if (k.getClass() == clazz) {
                entities.add(v.entity);
            }
        });

        return entities;
    }

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



    private class CacheObject {
        private final long createdTime = System.currentTimeMillis();
        private final /*BaseEntity*/ Object entity;

        private CacheObject(/*BaseEntity*/ Object entity) {
            this.entity = entity;
        }
    }


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
