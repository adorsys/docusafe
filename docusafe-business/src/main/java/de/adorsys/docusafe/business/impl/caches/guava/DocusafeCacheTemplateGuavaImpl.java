package de.adorsys.docusafe.business.impl.caches.guava;

import de.adorsys.docusafe.business.impl.caches.DocusafeCacheTemplate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Created by peter on 14.08.18 at 17:21.
 */
public class DocusafeCacheTemplateGuavaImpl <K,V> implements DocusafeCacheTemplate<K,V> {
    private Cache<K, V> map =
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .build();

    @Override
    public V get(K key) {
        return map.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public void remove(K key) {
        map.invalidate(key);
    }

    @Override
    public long size() {
        return map.size();
    }

}
