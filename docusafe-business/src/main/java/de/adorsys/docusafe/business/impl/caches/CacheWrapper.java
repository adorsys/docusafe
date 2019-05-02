package de.adorsys.docusafe.business.impl.caches;

import de.adorsys.docusafe.business.impl.caches.guava.UserAuthCacheGuavaImpl;
import de.adorsys.docusafe.business.impl.caches.guava.UserDFSCredentialsCacheGuavaImpl;
import de.adorsys.docusafe.business.impl.caches.guava.UserPathSecretKeyCacheGuavaImpl;
import de.adorsys.docusafe.business.impl.caches.guava.UserPublicKeyListCacheGuavaImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class CacheWrapper {

    private UserAuthCache userAuthCache = new UserAuthCacheGuavaImpl();
    private UserDFSCredentialsCache userDFSCredentialsCache = new UserDFSCredentialsCacheGuavaImpl();
    private UserPathSecretKeyCache userPathSecretKeyCache = new UserPathSecretKeyCacheGuavaImpl();
    private UserPublicKeyListCache userPublicKeyListCache = new UserPublicKeyListCacheGuavaImpl();

    @Override
    public String toString() {
        return "CacheWrapper{" +
                "userAuthCache=" + userAuthCache +
                ", userDFSCredentialsCache=" + userDFSCredentialsCache +
                ", userPathSecretKeyCache=" + userPathSecretKeyCache +
                ", userPublicKeyListCache=" + userPublicKeyListCache +
                '}';
    }
}
