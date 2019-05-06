package de.adorsys.docusafe.business.impl.caches.guava;

import de.adorsys.docusafe.business.impl.caches.UserAuthCacheKey;
import de.adorsys.docusafe.business.impl.caches.UsersPrivateKeyStoreCache;
import de.adorsys.docusafe.service.api.keystore.types.KeyStoreAccess;

public class UsersPrivateKeyStoreCacheGuavaImpl extends DocusafeCacheTemplateGuavaImpl<UserAuthCacheKey, KeyStoreAccess> implements UsersPrivateKeyStoreCache {
}

