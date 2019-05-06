package de.adorsys.docusafe.business.impl.dummyCaches;

import de.adorsys.docusafe.business.impl.caches.UserAuthCacheKey;
import de.adorsys.docusafe.business.impl.caches.guava.UserPathSecretKeyCacheGuavaImpl;

import javax.crypto.SecretKey;

public class UserPathSecretKeyCacheNoCacheTestImpl extends UserPathSecretKeyCacheGuavaImpl {
    @Override
    public SecretKey get(UserAuthCacheKey key) {
        return null;
    }

    @Override
    public void put(UserAuthCacheKey key, SecretKey value) {
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

