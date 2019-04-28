package de.adorsys.docusafe.business.impl.caches.guava;


import de.adorsys.docusafe.business.impl.caches.UserAuthCache;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.UserID;

/**
 * Created by peter on 14.08.18 at 17:27.
 */
public class UserAuthCacheGuavaImpl
        extends DocusafeCacheTemplateGuavaImpl <UserID, ReadKeyPassword>
        implements UserAuthCache {
}
