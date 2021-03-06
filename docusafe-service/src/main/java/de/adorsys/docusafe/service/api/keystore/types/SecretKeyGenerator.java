package de.adorsys.docusafe.service.api.keystore.types;

import javax.security.auth.callback.CallbackHandler;

/**
 * Created by peter on 26.02.18 at 17:03.
 */
public interface SecretKeyGenerator {
    SecretKeyEntry generate(String alias, CallbackHandler secretKeyPassHandler);
}
