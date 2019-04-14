package org.adorsys.docusafe.business.impl;

import com.amazonaws.services.s3.model.Bucket;
import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.dfs.connection.api.domain.Payload;
import de.adorsys.dfs.connection.api.filesystem.FilesystemConnectionPropertiesImpl;
import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.dfs.connection.api.service.impl.SimplePayloadImpl;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.dfs.connection.api.types.properties.ConnectionProperties;
import de.adorsys.dfs.connection.impl.amazons3.AmazonS3ConnectionProperitesImpl;
import de.adorsys.dfs.connection.impl.factory.ReadArguments;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.exceptions.UserExistsException;
import org.adorsys.docusafe.business.types.MoveType;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DSDocumentStream;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.service.api.bucketpathencryption.BucketPathEncryptionService;
import org.adorsys.docusafe.service.api.keystore.KeyStoreService;
import org.adorsys.docusafe.service.api.keystore.types.*;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.adorsys.docusafe.service.impl.bucketpathencryption.BucketPathEncryptionServiceImpl;
import org.adorsys.docusafe.service.impl.keystore.service.KeyStoreServiceImpl;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.util.List;

public class DocumentSafeServiceImpl implements DocumentSafeService {
    private final Class2JsonHelper class2JsonHelper = new Class2JsonHelper();
    private final DFSConnection systemDFS;
    private final BucketPathEncryptionService bucketPathEncryptionService = new BucketPathEncryptionServiceImpl();
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
            if (userExists(userIDAuth.getUserID())) {
                throw new UserExistsException(userIDAuth.getUserID());
            }
            systemDFS.createContainer(FolderHelper.getRootDirectory(userIDAuth.getUserID()));
            KeyStoreAuth keyStoreAuth = new KeyStoreAuth(new ReadStorePassword(userIDAuth.getReadKeyPassword().getValue()), userIDAuth.getReadKeyPassword());
            KeyStore usersSystemKeyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, new KeyStoreCreationConfig(1, 0, 1));

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            usersSystemKeyStore.store(stream, userIDAuth.getReadKeyPassword().getValue().toCharArray());
            Payload payload = new SimplePayloadImpl(stream.toByteArray());
            // KeyStoreAccess keyStoreAccess = new KeyStoreAccess(usersSystemKeyStore, keyStoreAuth);
            // SecretKeyIDWithKey randomSecretKeyID = keyStoreService.getRandomSecretKeyID(keyStoreAccess);

            systemDFS.putBlob(FolderHelper.getKeyStorePath(userIDAuth.getUserID()), payload);
            BucketPath dfsCredentialsPath = FolderHelper.getDFSCredentialsPath(userIDAuth.getUserID());
            DocumentContent dfsCredentialsAsDocumentContent = class2JsonHelper.dfsCredentialsToContent(defaultUserDFSCredentials);

            systemDFS.putBlob(dfsCredentialsPath, new SimplePayloadImpl(dfsCredentialsAsDocumentContent.getValue()));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public void destroyUser(UserIDAuth userIDAuth) {

    }

    @Override
    public boolean userExists(UserID userID) {
        return (systemDFS.containerExists(FolderHelper.getRootDirectory(userID)));
    }

    @Override
    public void storeDocument(UserIDAuth userIDAuth, DSDocument dsDocument) {

    }

    @Override
    public DSDocument readDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return null;
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

    }

    @Override
    public boolean documentExists(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return false;
    }

    @Override
    public void deleteFolder(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN) {

    }

    @Override
    public List<DocumentFQN> list(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
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
    public void moveDocumnetToInboxOfUser(UserIDAuth userIDAuth, UserID receiverUserID, DocumentFQN sourceDocumentFQN, DocumentFQN destDocumentFQN, MoveType moveType) {

    }

    @Override
    public DSDocument moveDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source, DocumentFQN destination) {
        return null;
    }
}
