package org.adorsys.docusafe.service.impl.keystore.service;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.common.utils.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.service.api.keystore.KeyStoreService;
import org.adorsys.docusafe.service.api.keystore.types.*;
import org.adorsys.docusafe.service.impl.keystore.generator.KeyStoreGenerator;
import org.apache.commons.lang3.RandomUtils;

import javax.crypto.SecretKey;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

@Slf4j
public class KeyStoreServiceImpl implements KeyStoreService {

    @Override
    public KeyStore createKeyStore(KeyStoreAuth keyStoreAuth,
                                   KeyStoreType keyStoreType,
                                   KeyStoreCreationConfig config) {
        try {
            log.debug("start create keystore ");
            if (config == null) {
                config = new KeyStoreCreationConfig(5, 5, 5);
            }
            // TODO, hier also statt der StoreID nun das
            String serverKeyPairAliasPrefix = HexUtil.convertBytesToHexString(UUID.randomUUID().toString().getBytes());
            log.debug("keystoreid = " + serverKeyPairAliasPrefix);
            {
                String realKeyStoreId = new String(HexUtil.convertHexStringToBytes(serverKeyPairAliasPrefix));
                log.debug("meaning of keystoreid = " + realKeyStoreId);
            }
            KeyStoreGenerator keyStoreGenerator = new KeyStoreGenerator(
                    config,
                    keyStoreType,
                    serverKeyPairAliasPrefix,
                    keyStoreAuth.getReadKeyPassword());
            KeyStore userKeyStore = keyStoreGenerator.generate();
            log.debug("finished create keystore ");
            return userKeyStore;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public PublicKeyList getPublicKeys(KeyStoreAccess keyStoreAccess) {
        try {
            log.debug("get public keys");
            PublicKeyList result = new PublicKeyList();
            KeyStore keyStore = keyStoreAccess.getKeyStore();
            for (Enumeration<String> keyAliases = keyStore.aliases(); keyAliases.hasMoreElements(); ) {
                final String keyAlias = keyAliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);
                if (cert == null) continue; // skip
                boolean[] keyUsage = cert.getKeyUsage();
                // digitalSignature (0), nonRepudiation (1), keyEncipherment (2), dataEncipherment (3),
                // keyAgreement (4), keyCertSign (5), cRLSign (6), encipherOnly (7), decipherOnly (8)
                if (keyUsage[2] || keyUsage[3] || keyUsage[4]) {
                    result.add(new PublicKeyIDWithPublicKey(new KeyID(keyAlias), cert.getPublicKey()));
                }
            }
            return result;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public PrivateKey getPrivateKey(KeyStoreAccess keyStoreAccess, KeyID keyID) {
        try {
            ReadKeyPassword readKeyPassword = keyStoreAccess.getKeyStoreAuth().getReadKeyPassword();
            KeyStore keyStore = keyStoreAccess.getKeyStore();
            PrivateKey privateKey;
            privateKey = (PrivateKey) keyStore.getKey(keyID.getValue(), readKeyPassword.getValue().toCharArray());
            return privateKey;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public SecretKey getSecretKey(KeyStoreAccess keyStoreAccess, KeyID keyID) {
        try {
            KeyStore keyStore = keyStoreAccess.getKeyStore();
            SecretKey key = null;
            char[] password = keyStoreAccess.getKeyStoreAuth().getReadKeyPassword().getValue().toCharArray();
            return (SecretKey) keyStore.getKey(keyID.getValue(), password);
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public SecretKeyIDWithKey getRandomSecretKeyID(KeyStoreAccess keyStoreAccess) {
        try {
            KeyStore keyStore = keyStoreAccess.getKeyStore();
            Key key = null;
            String randomAlias = null;
            Enumeration<String> aliases = keyStore.aliases();
            List<String> keyIDs = new ArrayList<>();
            for (String keyAlias : Collections.list(aliases)) {
                if (keyStore.entryInstanceOf(keyAlias, KeyStore.SecretKeyEntry.class)) {
                    keyIDs.add(keyAlias);
                }
            }
            if (keyIDs.size() == 0) {
                throw new BaseException("No secret keys in the keystore");
            }
            int randomIndex = RandomUtils.nextInt(0, keyIDs.size());
            randomAlias = keyIDs.get(randomIndex);
            char[] password = keyStoreAccess.getKeyStoreAuth().getReadKeyPassword().getValue().toCharArray();
            key = keyStore.getKey(randomAlias, password);
            return new SecretKeyIDWithKey(new KeyID(randomAlias), (SecretKey) key);
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw BaseExceptionHandler.handle(e);
        }
    }
}
