package org.adorsys.docusafe.business.impl.caches.guava;


import org.adorsys.docusafe.business.impl.caches.UserAuthCache;
import org.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import org.adorsys.docusafe.service.api.types.UserID;

/**
 * Created by peter on 14.08.18 at 17:27.
 */
public class UserAuthCacheGuavaImpl
        extends DocusafeCacheTemplateGuavaImpl <UserID, ReadKeyPassword>
        implements UserAuthCache {
}
