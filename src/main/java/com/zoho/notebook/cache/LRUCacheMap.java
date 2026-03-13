package com.zoho.notebook.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LRUCacheMap<K, V> {

    private final int maxEntries;
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<K> accessOrder = new ConcurrentLinkedDeque<>();

    public LRUCacheMap(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public void put(K key, V value) {
        if (key == null)
        	return;

        cache.put(key, value);
        // Move to end of queue (Most Recently Used)
        accessOrder.remove(key); 
        accessOrder.addLast(key);

        evictIfNeeded();
    }

    public V get(K key) {
        if (key == null) 
        	return null;

        V value = cache.get(key);
        if (value != null) {
            // Update access order: remove from current pos and move to end
            accessOrder.remove(key);
            accessOrder.addLast(key);
        }
        return value;
    }

    public boolean contains(K key) {
        return key != null && cache.containsKey(key);
    }

    public void remove(K key) {
        if (key == null)
        	return;
        cache.remove(key);
        accessOrder.remove(key);
    }

    private void evictIfNeeded() {
        while (cache.size() > maxEntries) {
            K oldestKey = accessOrder.pollFirst();
            if (oldestKey != null) {
                cache.remove(oldestKey);
            } else {
                break;
            }
        }
    }
}