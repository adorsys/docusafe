package org.adorsys.docusafe.business.impl;

import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.dfs.connection.api.domain.Payload;
import de.adorsys.dfs.connection.api.filesystem.FilesystemConnectionPropertiesImpl;
import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.dfs.connection.api.service.impl.SimplePayloadImpl;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.dfs.connection.api.types.properties.ConnectionProperties;
import de.adorsys.dfs.connection.impl.amazons3.AmazonS3ConnectionProperitesImpl;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.dfs.connection.impl.factory.ReadArguments;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.exceptions.UserExistsException;
import org.adorsys.docusafe.business.types.MoveType;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DSDocumentStream;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.service.api.bucketpathencryption.BucketPathEncryptionService;
import org.adorsys.docusafe.service.api.cmsencryption.CMSEncryptionService;
import org.adorsys.docusafe.service.api.keystore.KeyStoreService;
import org.adorsys.docusafe.service.api.keystore.types.*;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.adorsys.docusafe.service.impl.bucketpathencryption.BucketPathEncryptionServiceImpl;
import org.adorsys.docusafe.service.impl.cmsencryption.services.CMSEncryptionServiceImpl;
import org.adorsys.docusafe.service.impl.keystore.service.KeyStoreServiceImpl;
import org.bouncycastle.cms.CMSEnvelopedData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Random;

@Slf4j
public class DocumentSafeServiceImpl implements DocumentSafeService {
    private final DFSConnection systemDFS;
    private final Class2JsonHelper class2JsonHelper = new Class2JsonHelper();
    private final BucketPathEncryptionService bucketPathEncryptionService = new BucketPathEncryptionServiceImpl();
    private final CMSEncryptionService cmsEncryptionService = new CMSEncryptionServiceImpl();
    private final DFSCredentials defaultUserDFSCredentials = getDefaultDFSCredentials();

    private DFSCredentials getDefaultDFSCredentials() {
        ConnectionProperties props = new ReadArguments().readEnvironment();
        DFSCredentials dfsCredentials = new DFSCredentials();
        if (props instanceof FilesystemConnectionPropertiesImpl) {
            dfsCredentials.filesystem = (FilesystemConnectionPropertiesImpl) props;
        } else {
            dfsCredentials.amazons3 = (AmazonS3ConnectionProperitesImpl) props;
        }
        return dfsCredentials;
    }

    private final KeyStoreService keyStoreService = new KeyStoreServiceImpl();

    public DocumentSafeServiceImpl(DFSConnection dfsConnection) {
        systemDFS = dfsConnection;
    }

    @Override
    public void createUser(UserIDAuth userIDAuth) {
        try {
            // create userspace in systemdfs
            if (userExists(userIDAuth.getUserID())) {
                throw new UserExistsException(userIDAuth.getUserID());
            }
            // create and persist public keystore
            KeyStoreAccess publicKeyStoreAccess = null;
            {
                KeyStoreAuth keyStoreAuth = new KeyStoreAuth(new ReadStorePassword(userIDAuth.getReadKeyPassword().getValue()), userIDAuth.getReadKeyPassword());
                KeyStore usersSystemKeyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, new KeyStoreCreationConfig(1, 0, 0));
                persistKeystore(userIDAuth, usersSystemKeyStore, systemDFS);
                publicKeyStoreAccess = new KeyStoreAccess(usersSystemKeyStore, keyStoreAuth);
            }

            // create and persist encrypted dfscredentials
            DFSCredentials userDFSCredentials = null;
            {
                userDFSCredentials = defaultUserDFSCredentials;
                userDFSCredentials.setRootBucket(userIDAuth.getUserID());
                // retrieve public key of public keystore once to encrypt DFSCredentials
                PublicKeyIDWithPublicKey publicKeyIDWithPublicKey = keyStoreService.getPublicKeys(publicKeyStoreAccess).get(0);
                BucketPath dfsCredentialsPath = FolderHelper.getDFSCredentialsPath(userIDAuth.getUserID());
                Payload payload = class2JsonHelper.dfsCredentialsToContent(userDFSCredentials);
                CMSEnvelopedData encryptedDFSCredentialsAsEnvelope = cmsEncryptionService.encrypt(payload, publicKeyIDWithPublicKey.getPublicKey(), publicKeyIDWithPublicKey.getKeyID());
                Payload encryptedPayload = new SimplePayloadImpl(encryptedDFSCredentialsAsEnvelope.getEncoded());
                systemDFS.putBlob(FolderHelper.getDFSCredentialsPath(userIDAuth.getUserID()), encryptedPayload);
            }

            // create users DFS
            DFSConnection usersDFSConnection = null;
            {
                usersDFSConnection = DFSConnectionFactory.get(userDFSCredentials.getProperties());
            }

            // create and persist private keystore
            KeyStoreAccess privateKeyStoreAccess = null;
            {
                KeyStoreAuth keyStoreAuth = new KeyStoreAuth(new ReadStorePassword(userIDAuth.getReadKeyPassword().getValue()), userIDAuth.getReadKeyPassword());
                KeyStore usersSystemKeyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, new KeyStoreCreationConfig(5, 0, 1));
                persistKeystore(userIDAuth, usersSystemKeyStore, usersDFSConnection);
                privateKeyStoreAccess = new KeyStoreAccess(usersSystemKeyStore, keyStoreAuth);
            }

            // extract public keys and store them in userspace of systemdfs
            {
                List<PublicKeyIDWithPublicKey> publicKeys = keyStoreService.getPublicKeys(privateKeyStoreAccess);
                Payload payload = class2JsonHelper.keyListToContent(publicKeys);
                systemDFS.putBlob(FolderHelper.getPublicKeyListPath(userIDAuth.getUserID()), payload);
            }


        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public void destroyUser(UserIDAuth userIDAuth) {
        final DFSConnection usersDFSConnection = getUsersDFS(userIDAuth);
        usersDFSConnection.deleteContainer(FolderHelper.getRootDirectory(userIDAuth.getUserID()));
        systemDFS.deleteContainer(FolderHelper.getRootDirectory(userIDAuth.getUserID()));
    }

    @Override
    public boolean userExists(UserID userID) {
        return (systemDFS.containerExists(FolderHelper.getRootDirectory(userID)));
    }

    @Override
    public void storeDocument(UserIDAuth userIDAuth, DSDocument dsDocument) {
        try {
            Payload payload = null;
            {
                // get random public key to encrypt
                Payload publicKeysAsPayload = systemDFS.getBlob(FolderHelper.getPublicKeyListPath(userIDAuth.getUserID()));
                List<PublicKeyIDWithPublicKey> publicKeys = class2JsonHelper.contentToKeyList(publicKeysAsPayload);
                Random random = new Random();
                int r = random.nextInt(publicKeys.size());
                log.debug("size " + publicKeys.size() + " random:" + r);
                PublicKeyIDWithPublicKey publicKeyIDWithPublicKey = publicKeys.get(r);
                Payload unencryptedPayload = new SimplePayloadImpl(dsDocument.getDocumentContent().getValue());
                CMSEnvelopedData cmsEnvelope = cmsEncryptionService.encrypt(unencryptedPayload, publicKeyIDWithPublicKey.getPublicKey(), publicKeyIDWithPublicKey.getKeyID());
                payload = new SimplePayloadImpl(cmsEnvelope.getEncoded());
            }

            final DFSConnection usersDFSConnection = getUsersDFS(userIDAuth);
            SecretKeyIDWithKey pathEncryptionSecretKey = null;
            {
                KeyStoreAccess privateKeyStoreAccess = getKeyStoreAccess(usersDFSConnection, userIDAuth);
                pathEncryptionSecretKey = keyStoreService.getRandomSecretKeyID(privateKeyStoreAccess);
            }

            BucketPath unencryptedPath = FolderHelper.getHomeDirectory(userIDAuth.getUserID()).appendName(dsDocument.getDocumentFQN().getValue());
            BucketPath encryptedBucketPath = bucketPathEncryptionService.encrypt(pathEncryptionSecretKey.getSecretKey(), unencryptedPath);

            usersDFSConnection.putBlob(encryptedBucketPath, payload);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public DSDocument readDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        try {
            DFSAndKeystoreAndPath dfsAndKeystoreAndPath = getUsersAccess(userIDAuth, documentFQN);
            Payload payload = dfsAndKeystoreAndPath.usersDFS.getBlob(dfsAndKeystoreAndPath.encryptedBucketPath);
            CMSEnvelopedData cmsEnvelopedData = new CMSEnvelopedData(payload.getData());
            Payload decrypt = cmsEncryptionService.decrypt(cmsEnvelopedData, dfsAndKeystoreAndPath.privateKeystoreAccess);
            return new DSDocument(documentFQN, new DocumentContent(decrypt.getData()));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public void storeDocumentStream(UserIDAuth userIDAuth, DSDocumentStream dsDocumentStream) {

    }

    @Override
    public DSDocumentStream readDocumentStream(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return null;
    }

    @Override
    public void deleteDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
                DFSAndKeystoreAndPath dfsAndKeystoreAndPath = getUsersAccess(userIDAuth, documentFQN);
                dfsAndKeystoreAndPath.usersDFS.removeBlob(dfsAndKeystoreAndPath.encryptedBucketPath);
    }

    @Override
    public boolean documentExists(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        DFSAndKeystoreAndPath dfsAndKeystoreAndPath = getUsersAccess(userIDAuth, documentFQN);
        return dfsAndKeystoreAndPath.usersDFS.blobExists(dfsAndKeystoreAndPath.encryptedBucketPath);
    }

    @Override
    public void deleteFolder(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN) {

    }

    @Override
    public List<DocumentFQN> list(UserIDAuth userIDAuth, DocumentDirectoryFQN
            documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
        return null;
    }

    @Override
    public List<DocumentFQN> listInbox(UserIDAuth userIDAuth) {
        return null;
    }

    @Override
    public void writeDocumentToInboxOfUser(UserID receiverUserID, DSDocument document, DocumentFQN destDocumentFQN) {

    }

    @Override
    public DSDocument readDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source) {
        return null;
    }

    @Override
    public void deleteDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN documentFQN) {

    }

    @Override
    public void moveDocumnetToInboxOfUser(UserIDAuth userIDAuth, UserID receiverUserID, DocumentFQN
            sourceDocumentFQN, DocumentFQN destDocumentFQN, MoveType moveType) {

    }

    @Override
    public DSDocument moveDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source, DocumentFQN destination) {
        return null;
    }


    private void persistKeystore(UserIDAuth userIDAuth, KeyStore usersSystemKeyStore, DFSConnection systemDFS) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        usersSystemKeyStore.store(stream, userIDAuth.getReadKeyPassword().getValue().toCharArray());
        Payload payload = new SimplePayloadImpl(stream.toByteArray());
        systemDFS.createContainer(FolderHelper.getRootDirectory(userIDAuth.getUserID()));
        systemDFS.putBlob(FolderHelper.getKeyStorePath(userIDAuth.getUserID()), payload);
    }

    private DFSConnection getUsersDFS(UserIDAuth userIDAuth) {
        try {
            KeyStoreAccess publicKeyStoreAccess = getKeyStoreAccess(systemDFS, userIDAuth);

            DFSCredentials userDFSCredentials = null;
            {
                // retrieve DFS
                Payload encryptedPayload = systemDFS.getBlob(FolderHelper.getDFSCredentialsPath(userIDAuth.getUserID()));
                CMSEnvelopedData cmsEnvelopedData = new CMSEnvelopedData(encryptedPayload.getData());
                Payload decrypt = cmsEncryptionService.decrypt(cmsEnvelopedData, publicKeyStoreAccess);
                userDFSCredentials = class2JsonHelper.contentToDFSConnection(decrypt);
            }
            return DFSConnectionFactory.get(userDFSCredentials.getProperties());

        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }

    }

    private KeyStoreAccess getKeyStoreAccess(DFSConnection dfs, UserIDAuth userIDAuth) {
        try {
            KeyStoreAccess keyStoreAccess = null;
            {
                KeyStoreAuth keyStoreAuth = new KeyStoreAuth(new ReadStorePassword(userIDAuth.getReadKeyPassword().getValue()), userIDAuth.getReadKeyPassword());
                Payload payload = dfs.getBlob(FolderHelper.getKeyStorePath(userIDAuth.getUserID()));
                ByteArrayInputStream in = new ByteArrayInputStream(payload.getData());
                KeyStore keystore = KeyStore.getInstance(KeyStoreType.DEFAULT.getValue());
                keystore.load(in, userIDAuth.getReadKeyPassword().getValue().toCharArray());
                keyStoreAccess = new KeyStoreAccess(keystore, keyStoreAuth);
            }
            return keyStoreAccess;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    private DFSAndKeystoreAndPath getUsersAccess(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        DFSAndKeystoreAndPath dfsAndKeystoreAndPath = new DFSAndKeystoreAndPath();
        dfsAndKeystoreAndPath.usersDFS = getUsersDFS(userIDAuth);
        SecretKeyIDWithKey pathEncryptionSecretKey = null;
        {
            KeyStoreAccess privateKeyStoreAccess = getKeyStoreAccess(dfsAndKeystoreAndPath.usersDFS, userIDAuth);
            pathEncryptionSecretKey = keyStoreService.getRandomSecretKeyID(privateKeyStoreAccess);
        }
        BucketPath unencryptedPath = FolderHelper.getHomeDirectory(userIDAuth.getUserID()).appendName(documentFQN.getValue());
        dfsAndKeystoreAndPath.encryptedBucketPath = bucketPathEncryptionService.encrypt(pathEncryptionSecretKey.getSecretKey(), unencryptedPath);
        dfsAndKeystoreAndPath.privateKeystoreAccess = getKeyStoreAccess(dfsAndKeystoreAndPath.usersDFS, userIDAuth);
        return dfsAndKeystoreAndPath;
    }

    private final static class DFSAndKeystoreAndPath {
        DFSConnection usersDFS;
        KeyStoreAccess privateKeystoreAccess;
        BucketPath encryptedBucketPath;

    }
}
