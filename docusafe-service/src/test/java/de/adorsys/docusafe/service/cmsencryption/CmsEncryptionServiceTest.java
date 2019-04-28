package de.adorsys.docusafe.service.cmsencryption;


import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.domain.Payload;
import de.adorsys.dfs.connection.api.service.impl.SimplePayloadImpl;
import de.adorsys.docusafe.service.api.keystore.types.*;
import de.adorsys.docusafe.service.impl.cmsencryption.services.CMSEncryptionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import de.adorsys.docusafe.service.api.cmsencryption.CMSEncryptionService;
import de.adorsys.docusafe.service.api.keystore.KeyStoreService;
import de.adorsys.docusafe.service.impl.keystore.service.KeyStoreServiceImpl;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;

@Slf4j
public class CmsEncryptionServiceTest {

        private CMSEncryptionService cmsEncryptionService = new CMSEncryptionServiceImpl();
        private KeyStoreService keyStoreService = new KeyStoreServiceImpl();

        @Test
        public void cmsEnvelopeEncryptAndDecryptTest() {

            ReadKeyPassword readKeyPassword = new ReadKeyPassword("readkeypassword");
            ReadStorePassword readStorePassword = new ReadStorePassword("readstorepassword");
            KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);

            KeyStoreCreationConfig config = new KeyStoreCreationConfig(1, 0, 1);
            KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
            KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

            PublicKeyIDWithPublicKey publicKeyIDWithPublicKey = keyStoreService.getPublicKeys(keyStoreAccess).get(0);
            PublicKey publicKey = publicKeyIDWithPublicKey.getPublicKey();
            KeyID keyID = publicKeyIDWithPublicKey.getKeyID();

            Payload origMessage = new SimplePayloadImpl("message content".getBytes());
            CMSEnvelopedData encrypted  = cmsEncryptionService.encrypt(origMessage, publicKey, keyID);
            Payload decrypted = cmsEncryptionService.decrypt(encrypted, keyStoreAccess);

            Assert.assertArrayEquals(origMessage.getData(),decrypted.getData());
            log.debug("en and decrypted successfully");
        }


    @Test
    public void cmsEnvelopeStreamEncryptAndDecryptTest() {
            try {

                ReadKeyPassword readKeyPassword = new ReadKeyPassword("readkeypassword");
                ReadStorePassword readStorePassword = new ReadStorePassword("readstorepassword");
                KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);

                KeyStoreCreationConfig config = new KeyStoreCreationConfig(1, 0, 0);
                KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
                KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

                PublicKeyIDWithPublicKey publicKeyIDWithPublicKey = keyStoreService.getPublicKeys(keyStoreAccess).get(0);
                PublicKey publicKey = publicKeyIDWithPublicKey.getPublicKey();
                KeyID keyID = publicKeyIDWithPublicKey.getKeyID();

                byte[] origMessage = "message content".getBytes();
                InputStream origStream = new ByteArrayInputStream(origMessage);
                InputStream encryptedStream = cmsEncryptionService.buildEncryptionInputStream(origStream, publicKey, keyID);
                InputStream decrytedStream = cmsEncryptionService.buildDecryptionInputStream(encryptedStream, keyStoreAccess);
                byte[] decrypted = IOUtils.toByteArray(decrytedStream);
                IOUtils.closeQuietly(decrytedStream);
                IOUtils.closeQuietly(encryptedStream);
                IOUtils.closeQuietly(origStream);

                Assert.assertArrayEquals(origMessage, decrypted);
                log.debug("en and decrypted successfully");
            } catch (Exception e) {
                throw BaseExceptionHandler.handle(e);
            }
    }

    @Test
    public void cmsEnvelopeStreamSymmetricEncryptAndDecryptTest() {
        try {

            ReadKeyPassword readKeyPassword = new ReadKeyPassword("readkeypassword");
            ReadStorePassword readStorePassword = new ReadStorePassword("readstorepassword");
            KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);

            KeyStoreCreationConfig config = new KeyStoreCreationConfig(0, 0, 1);
            KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
            KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

            SecretKeyIDWithKey randomSecretKeyID = keyStoreService.getRandomSecretKeyID(keyStoreAccess);

            byte[] origMessage = "message content".getBytes();
            InputStream origStream = new ByteArrayInputStream(origMessage);
            InputStream encryptedStream = cmsEncryptionService.buildEncryptionInputStream(origStream, randomSecretKeyID.getSecretKey(), randomSecretKeyID.getKeyID());
            InputStream decrytedStream = cmsEncryptionService.buildDecryptionInputStream(encryptedStream, keyStoreAccess);
            byte[] decrypted = IOUtils.toByteArray(decrytedStream);
            IOUtils.closeQuietly(decrytedStream);
            IOUtils.closeQuietly(encryptedStream);
            IOUtils.closeQuietly(origStream);

            Assert.assertArrayEquals(origMessage, decrypted);
            log.debug("en and decrypted successfully");
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }
}
