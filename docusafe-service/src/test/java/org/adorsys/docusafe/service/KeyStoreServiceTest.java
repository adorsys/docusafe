package org.adorsys.docusafe.service;

import org.adorsys.cryptoutils.exceptions.BaseExceptionHandler;
import org.adorsys.encobject.complextypes.BucketDirectory;
import org.adorsys.encobject.complextypes.BucketPath;
import org.adorsys.encobject.domain.KeyStoreAccess;
import org.adorsys.encobject.domain.KeyStoreAuth;
import org.adorsys.encobject.domain.ReadKeyPassword;
import org.adorsys.encobject.domain.ReadStorePassword;
import org.adorsys.encobject.service.api.ContainerPersistence;
import org.adorsys.encobject.service.api.ExtendedStoreConnection;
import org.adorsys.encobject.service.api.KeyStoreService;
import org.adorsys.encobject.service.impl.ContainerPersistenceImpl;
import org.adorsys.encobject.service.impl.KeyStoreServiceImpl;
import org.adorsys.encobject.service.impl.generator.KeyStoreCreationConfigImpl;
import org.adorsys.jkeygen.keystore.KeyStoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;

/**
 * Created by peter on 02.01.18.
 */
public class KeyStoreServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(KeyStoreServiceTest.class);

    private static String keystoreContainer = "keystorecontainer";
    private ExtendedStoreConnection extendedStoreConnection;


    public KeyStoreServiceTest(ExtendedStoreConnection extendedStoreConnection) {
        this.extendedStoreConnection = extendedStoreConnection;
    }

    public KeyStoreStuff createKeyStore() {
        return createKeyStore(keystoreContainer, new ReadStorePassword("storePassword"), new ReadKeyPassword("keypassword"), "key-store-id-123", null);
    }

    public KeyStoreStuff createKeyStore(String keystoreContainer,
                                        ReadStorePassword readStorePassword,
                                        ReadKeyPassword readKeyPassword,
                                        String keyStoreID,
                                        KeyStoreCreationConfigImpl config) {
        try {
            BucketDirectory keyStoreDirectory = new BucketDirectory(keystoreContainer);

            ContainerPersistence containerPersistence = new ContainerPersistenceImpl(extendedStoreConnection);
            try {
                // sollte der container exsitieren, ignorieren wir die Exception, um zu
                // sehen, ob sich ein keystore überschreiben lässt
                containerPersistence.createContainer(keyStoreDirectory);
            } catch (Exception e) {
                LOGGER.error("Exception is ignored");
            }
            AllServiceTest.buckets.add(keyStoreDirectory);

            KeyStoreService keyStoreService = new KeyStoreServiceImpl(extendedStoreConnection);
            KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);
            BucketPath keyStorePath = keyStoreDirectory.appendName(keyStoreID);
            keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, keyStorePath, config);
            KeyStore keyStore = keyStoreService.loadKeystore(keyStorePath, keyStoreAuth.getReadStoreHandler());
            return new KeyStoreStuff(keyStore, extendedStoreConnection, new KeyStoreAccess(keyStorePath, keyStoreAuth));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }


    public static class KeyStoreStuff {
        public final KeyStore keyStore;
        public final ExtendedStoreConnection extendedStoreConnection;
        public final KeyStoreAccess keyStoreAccess;


        public KeyStoreStuff(KeyStore keyStore, ExtendedStoreConnection extendedStoreConnection, KeyStoreAccess keyStoreAccess) {
            this.keyStore = keyStore;
            this.extendedStoreConnection = extendedStoreConnection;
            this.keyStoreAccess = keyStoreAccess;
        }
    }
}
