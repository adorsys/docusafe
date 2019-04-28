package de.adorsys.docusafe.business.impl.caches;

/**
 * Created by peter on 14.08.18 at 16:13.
 */
public interface DocusafeCacheTemplate<K,V> {
    V get(K key);
    void put(K key, V value);
    void remove(K key);
    long size();
}
