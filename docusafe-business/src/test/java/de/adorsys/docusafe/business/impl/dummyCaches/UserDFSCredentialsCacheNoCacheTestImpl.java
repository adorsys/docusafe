package de.adorsys.docusafe.business.impl.dummyCaches;

import de.adorsys.docusafe.business.impl.caches.UserAuthCacheKey;
import de.adorsys.docusafe.business.impl.caches.guava.UserDFSCredentialsCacheGuavaImpl;
import de.adorsys.docusafe.business.types.DFSCredentials;

public class UserDFSCredentialsCacheNoCacheTestImpl extends UserDFSCredentialsCacheGuavaImpl {
    @Override
    public DFSCredentials get(UserAuthCacheKey key) {
        return null;
    }

    @Override
    public void put(UserAuthCacheKey key, DFSCredentials value) {
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

