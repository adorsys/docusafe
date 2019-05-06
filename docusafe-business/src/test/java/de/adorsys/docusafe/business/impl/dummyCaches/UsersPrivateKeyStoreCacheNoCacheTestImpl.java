package de.adorsys.docusafe.business.impl.dummyCaches;

import de.adorsys.docusafe.business.impl.caches.UserAuthCacheKey;
import de.adorsys.docusafe.business.impl.caches.guava.UsersPrivateKeyStoreCacheGuavaImpl;
import de.adorsys.docusafe.service.api.keystore.types.KeyStoreAccess;

public class UsersPrivateKeyStoreCacheNoCacheTestImpl extends UsersPrivateKeyStoreCacheGuavaImpl {
    @Override
    public KeyStoreAccess get(UserAuthCacheKey key) {
        return null;
    }

    @Override
    public void put(UserAuthCacheKey key, KeyStoreAccess value) {
    }

    @Override
    public void remove(UserAuthCacheKey key) {
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " size:" + this.size();
    }


}
