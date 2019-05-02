package de.adorsys.docusafe.business.impl.caches.guava;

import de.adorsys.docusafe.business.impl.caches.UserAuthCacheKey;
import de.adorsys.docusafe.business.impl.caches.UserPathSecretKeyCache;

import javax.crypto.SecretKey;

public class UserPathSecretKeyCacheGuavaImpl extends DocusafeCacheTemplateGuavaImpl<UserAuthCacheKey, SecretKey> implements UserPathSecretKeyCache {
}
