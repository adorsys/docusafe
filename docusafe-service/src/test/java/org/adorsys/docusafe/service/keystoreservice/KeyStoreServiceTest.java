package org.adorsys.docusafe.service.keystoreservice;

import com.googlecode.catchexception.CatchException;
import org.adorsys.docusafe.service.api.keystore.KeyStoreService;
import org.adorsys.docusafe.service.api.keystore.types.*;
import org.adorsys.docusafe.service.impl.keystore.generator.KeyStoreCreationConfigImpl;
import org.adorsys.docusafe.service.impl.keystore.generator.KeyStoreServiceImplBaseFunctions;
import org.adorsys.docusafe.service.impl.keystore.generator.PasswordCallbackHandler;
import org.adorsys.docusafe.service.impl.keystore.generator.ProviderUtils;
import org.adorsys.docusafe.service.impl.keystore.service.KeyStoreServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class KeyStoreServiceTest {

    private KeyStoreService keyStoreService = new KeyStoreServiceImpl();
    private KeyStoreAuth keyStoreAuth;

    @Before
    public void setUp() {
        ReadStorePassword readStorePassword = new ReadStorePassword("keystorepass");
        ReadKeyPassword readKeyPassword = new ReadKeyPassword("keypass");
        keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);
    }

    @Test
    public void createKeyStore() throws Exception {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(1, 0, 1);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);

        Assert.assertNotNull(keyStore);

        List<String> list = Collections.list(keyStore.aliases());
        Assert.assertEquals(2, list.size());

        Assert.assertEquals("UBER", keyStore.getType());
        Assert.assertEquals(Security.getProvider("BC"), keyStore.getProvider());
    }

    @Test
    public void createKeyStoreEmptyConfig() throws Exception {
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, null);
        Assert.assertNotNull(keyStore);
        List<String> list = Collections.list(keyStore.aliases());
        Assert.assertEquals(15, list.size());
    }

    @Test
    public void createKeyStoreException() {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(0, 0, 0);

            CatchException.catchException(() ->keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config));
            Assert.assertTrue(CatchException.caughtException() != null);
    }

    @Test
    public void getPublicKeys() {
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, null);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        List<PublicKeyIDWithPublicKey> publicKeys = keyStoreService.getPublicKeys(keyStoreAccess);
        Assert.assertEquals(5, publicKeys.size());
    }

    @Test
    public void getPrivateKey() throws Exception {
        Provider bcProvider = ProviderUtils.bcProvider;
        KeyStore keyStore = KeyStoreServiceImplBaseFunctions.newKeyStore(KeyStoreType.DEFAULT); // UBER

        ReadKeyPassword readKeyPassword = new ReadKeyPassword("keypass");
        CallbackHandler readKeyHandler = new PasswordCallbackHandler(readKeyPassword.getValue().toCharArray());
        KeyStoreCreationConfigImpl keyStoreCreationConfig = new KeyStoreCreationConfigImpl(null);
        KeyPairGenerator encKeyPairGenerator = keyStoreCreationConfig.getEncKeyPairGenerator("KEYSTORE-ID-0");
        String alias = "KEYSTORE-ID-0" + UUID.randomUUID().toString();
        KeyPairEntry keyPairEntry = encKeyPairGenerator.generateEncryptionKey(alias, readKeyHandler);
        KeyStoreServiceImplBaseFunctions.addToKeyStore(keyStore, keyPairEntry);

        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

        String keyID = keyStore.aliases().nextElement();
        PrivateKey privateKey = keyStoreService.getPrivateKey(keyStoreAccess, new KeyID(keyID));
        System.out.println(privateKey);
        System.out.println(keyID);
        Assert.assertEquals(alias, keyID);
    }

    @Test
    public void getSecretKey() throws Exception {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(0, 0, 1);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

        String keyID = keyStore.aliases().nextElement();
        SecretKey secretKey = keyStoreService.getSecretKey(keyStoreAccess, new KeyID(keyID));
        Assert.assertNotNull(secretKey);
    }

    @Test
    public void getRandomSecretKeyID() {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(10, 10, 1);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        SecretKeyIDWithKey randomSecretKeyID = keyStoreService.getRandomSecretKeyID(keyStoreAccess);
        Assert.assertNotNull(randomSecretKeyID);
    }

    @Test
    public void getRandomSecretKeyIDException() {
        KeyStoreCreationConfig config = new KeyStoreCreationConfig(10, 10, 0);
        KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);
        CatchException.catchException(() ->keyStoreService.getRandomSecretKeyID(keyStoreAccess));
        Assert.assertTrue(CatchException.caughtException() != null);

    }
}