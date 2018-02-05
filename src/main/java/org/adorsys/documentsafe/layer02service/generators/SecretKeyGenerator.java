package org.adorsys.documentsafe.layer02service.generators;

import org.adorsys.jkeygen.keystore.SecretKeyData;
import org.adorsys.jkeygen.secretkey.SecretKeyBuilder;

import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;

public class SecretKeyGenerator {

    private final String secretKeyAlgo;
    private final Integer keySize;

    public SecretKeyGenerator(String secretKeyAlgo, Integer keySize) {
        this.secretKeyAlgo = secretKeyAlgo;
        this.keySize = keySize;
    }

    public SecretKeyData generate(String alias, CallbackHandler secretKeyPassHandler) {
        SecretKey secretKey = new SecretKeyBuilder()
                .withKeyAlg(secretKeyAlgo)
                .withKeyLength(keySize)
                .build();

        return SecretKeyData.builder().secretKey(secretKey).alias(alias).passwordSource(secretKeyPassHandler).keyAlgo(secretKeyAlgo).build();

    }
}
