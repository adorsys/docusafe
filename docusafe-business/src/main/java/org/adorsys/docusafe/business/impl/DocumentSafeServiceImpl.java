package org.adorsys.docusafe.business.impl;

import org.adorsys.cryptoutils.exceptions.BaseExceptionHandler;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.exceptions.NoWriteAccessException;
import org.adorsys.docusafe.business.exceptions.UserIDAlreadyExistsException;
import org.adorsys.docusafe.business.exceptions.UserIDDoesNotExistException;
import org.adorsys.docusafe.business.types.UserID;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DSDocumentMetaInfo;
import org.adorsys.docusafe.business.types.complex.DSDocumentStream;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.business.types.complex.DocumentLink;
import org.adorsys.docusafe.business.types.complex.DocumentLinkAsDSDocument;
import org.adorsys.docusafe.business.types.complex.UserIDAuth;
import org.adorsys.docusafe.business.utils.GrantUtil;
import org.adorsys.docusafe.business.utils.GuardUtil;
import org.adorsys.docusafe.business.utils.LinkUtil;
import org.adorsys.docusafe.business.utils.UserIDUtil;
import org.adorsys.docusafe.service.BucketService;
import org.adorsys.docusafe.service.DocumentGuardService;
import org.adorsys.docusafe.service.DocumentPersistenceService;
import org.adorsys.docusafe.service.impl.BucketServiceImpl;
import org.adorsys.docusafe.service.impl.DocumentGuardServiceImpl;
import org.adorsys.docusafe.service.impl.DocumentPersistenceServiceImpl;
import org.adorsys.docusafe.service.impl.GuardKeyType;
import org.adorsys.docusafe.service.types.AccessType;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.adorsys.docusafe.service.types.DocumentKeyID;
import org.adorsys.docusafe.service.types.complextypes.DocumentBucketPath;
import org.adorsys.docusafe.service.types.complextypes.DocumentGuardLocation;
import org.adorsys.docusafe.service.types.complextypes.DocumentKeyIDWithKeyAndAccessType;
import org.adorsys.encobject.complextypes.BucketDirectory;
import org.adorsys.encobject.complextypes.BucketPath;
import org.adorsys.encobject.domain.KeyStoreAccess;
import org.adorsys.encobject.domain.KeyStoreAuth;
import org.adorsys.encobject.domain.Payload;
import org.adorsys.encobject.domain.PayloadStream;
import org.adorsys.encobject.domain.UserMetaData;
import org.adorsys.encobject.service.api.ExtendedStoreConnection;
import org.adorsys.encobject.service.api.KeyStoreService;
import org.adorsys.encobject.service.impl.KeyStoreServiceImpl;
import org.adorsys.encobject.service.impl.SimplePayloadImpl;
import org.adorsys.encobject.service.impl.SimplePayloadStreamImpl;
import org.adorsys.encobject.service.impl.SimpleStorageMetadataImpl;
import org.adorsys.encobject.types.OverwriteFlag;
import org.adorsys.jkeygen.keystore.KeyStoreType;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 19.01.18 at 14:39.
 */
public class DocumentSafeServiceImpl implements DocumentSafeService {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentSafeServiceImpl.class);
    public static final String LINK_KEY = "DocumentSafeServiceImpl.LINK_KEY";

    private BucketService bucketService;
    private KeyStoreService keyStoreService;
    private DocumentGuardService documentGuardService;
    private DocumentPersistenceService documentPersistenceService;

    public DocumentSafeServiceImpl(ExtendedStoreConnection extendedStoreConnection) {
        bucketService = new BucketServiceImpl(extendedStoreConnection);
        keyStoreService = new KeyStoreServiceImpl(extendedStoreConnection);
        documentGuardService = new DocumentGuardServiceImpl(extendedStoreConnection);
        documentPersistenceService = new DocumentPersistenceServiceImpl(extendedStoreConnection);
    }

    /**
     * USER
     * ===========================================================================================
     */

    @Override
    public void createUser(UserIDAuth userIDAuth) {
        LOGGER.info("start create user for " + userIDAuth);

        {   // check user does not exist yet
            BucketDirectory userRootBucketDirectory = UserIDUtil.getUserRootBucketDirectory(userIDAuth.getUserID());
            if (bucketService.bucketExists(userRootBucketDirectory)) {
                throw new UserIDAlreadyExistsException(userIDAuth.getUserID().toString());
            }
        }
        KeyStoreAccess keyStoreAccess = null;
        {   // create KeyStore
            BucketDirectory keyStoreDirectory = UserIDUtil.getKeyStoreDirectory(userIDAuth.getUserID());
            KeyStoreAuth keyStoreAuth = UserIDUtil.getKeyStoreAuth(userIDAuth);
            bucketService.createBucket(keyStoreDirectory);
            BucketPath keyStorePath = UserIDUtil.getKeyStorePath(userIDAuth.getUserID());
            keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, keyStorePath, null);
            keyStoreAccess = new KeyStoreAccess(keyStorePath, keyStoreAuth);
        }
        BucketDirectory userHomeBucketDirectory = UserIDUtil.getHomeBucketDirectory(userIDAuth.getUserID());
        {   // create homeBucket
            bucketService.createBucket(userHomeBucketDirectory);
            createGuardForBucket(keyStoreAccess, userHomeBucketDirectory, AccessType.WRITE);
        }
        {   // Now create a welcome file in the Home directory
            storeDocument(userIDAuth, createWelcomeDocument());
        }

        LOGGER.info("finished create user for " + userIDAuth);
    }

    @Override
    public void destroyUser(UserIDAuth userIDAuth) {
        LOGGER.info("start destroy user for " + userIDAuth);
        BucketDirectory userRootBucketDirectory = UserIDUtil.getUserRootBucketDirectory(userIDAuth.getUserID());
        {   // check user does not exist yet
            if (!bucketService.bucketExists(userRootBucketDirectory)) {
                throw new UserIDDoesNotExistException(userIDAuth.getUserID().toString());
            }
        }
        {
            checkUserKeyPassword(userIDAuth);
        }
        bucketService.destroyBucket(userRootBucketDirectory);
        LOGGER.info("finished destroy user for " + userIDAuth);
    }

    /**
     * DOCUMENT
     * ===========================================================================================
     */

    /**
     * -- byte orientiert --
     */
    @Override
    public void storeDocument(UserIDAuth userIDAuth, DSDocument dsDocument) {
        LOGGER.info("start storeDocument for " + userIDAuth + " " + dsDocument.getDocumentFQN());

        SimpleStorageMetadataImpl storageMetadata = new SimpleStorageMetadataImpl();
        storageMetadata.mergeUserMetadata(dsDocument.getDsDocumentMetaInfo());
        if (dsDocument instanceof DocumentLinkAsDSDocument) {
            storageMetadata.getUserMetadata().put(LINK_KEY, "TRUE");
        }
        storageMetadata.setSize(new Long(dsDocument.getDocumentContent().getValue().length));
        DocumentBucketPath documentBucketPath = getTheDocumentBucketPath(userIDAuth.getUserID(), dsDocument.getDocumentFQN());
        DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType = getOrCreateDocumentKeyIDwithKeyForBucketPath(userIDAuth, documentBucketPath.getBucketDirectory(), AccessType.WRITE);
        // Hier ist keine Prüfung des Schreibrechts notwendig
        documentPersistenceService.persistDocument(
                documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey(),
                documentBucketPath,
                OverwriteFlag.TRUE,
                new SimplePayloadImpl(storageMetadata, dsDocument.getDocumentContent().getValue()));
        LOGGER.info("finished storeDocument for " + userIDAuth + " " + dsDocument.getDocumentFQN());
    }

    @Override
    public DSDocument readDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        LOGGER.info("start readDocument for " + userIDAuth + " " + documentFQN);
        DocumentBucketPath documentBucketPath = getTheDocumentBucketPath(userIDAuth.getUserID(), documentFQN);
        KeyStoreAccess keyStoreAccess = getKeyStoreAccess(userIDAuth);
        Payload payload = documentPersistenceService.loadDocument(keyStoreAccess, documentBucketPath);
        UserMetaData userMetaData = payload.getStorageMetadata().getUserMetadata();
        if (userMetaData.find(LINK_KEY) != null) {
            LOGGER.info("start load link " + documentFQN);
            DocumentLink documentLink = LinkUtil.getDocumentLink(payload.getData());
            DocumentBucketPath sourceDocumentBucketPath = documentLink.getSourceDocumentBucketPath();
            payload = documentPersistenceService.loadDocument(keyStoreAccess, sourceDocumentBucketPath);
            LOGGER.info("finished load link " + documentFQN);
        }
        LOGGER.info("finished readDocument for " + userIDAuth + " " + documentFQN);
        return new DSDocument(documentFQN, new DocumentContent(payload.getData()), new DSDocumentMetaInfo(payload.getStorageMetadata().getUserMetadata()));
    }

    /**
     * -- stream orientiert --
     */
    @Override
    public void storeDocumentStream(UserIDAuth userIDAuth, DSDocumentStream dsDocumentStream) {
        LOGGER.info("start storeDocumentStream for " + userIDAuth + " " + dsDocumentStream.getDocumentFQN());

        SimpleStorageMetadataImpl storageMetadata = new SimpleStorageMetadataImpl();
        storageMetadata.mergeUserMetadata(dsDocumentStream.getDsDocumentMetaInfo());
        DocumentBucketPath documentBucketPath = getTheDocumentBucketPath(userIDAuth.getUserID(), dsDocumentStream.getDocumentFQN());
        DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType = getOrCreateDocumentKeyIDwithKeyForBucketPath(userIDAuth, documentBucketPath.getBucketDirectory(), AccessType.WRITE);
        // Hier ist keine Prüfung des Schreibrechts notwendig
        documentPersistenceService.persistDocumentStream(
                documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey(),
                documentBucketPath,
                OverwriteFlag.TRUE,
                new SimplePayloadStreamImpl(storageMetadata, dsDocumentStream.getDocumentStream()));
        LOGGER.info("finished storeDocument for " + userIDAuth + " " + dsDocumentStream.getDocumentFQN());
    }



    @Override
    public DSDocumentStream readDocumentStream(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        try {
            LOGGER.info("start readDocumentStream for " + userIDAuth + " " + documentFQN);
            DocumentBucketPath documentBucketPath = getTheDocumentBucketPath(userIDAuth.getUserID(), documentFQN);
            KeyStoreAccess keyStoreAccess = getKeyStoreAccess(userIDAuth);
            PayloadStream payloadStream = documentPersistenceService.loadDocumentStream(keyStoreAccess, documentBucketPath);
            UserMetaData userMetaData = payloadStream.getStorageMetadata().getUserMetadata();
            if (userMetaData.find(LINK_KEY) != null) {
                LOGGER.info("start load link " + documentFQN);
                DocumentLink documentLink = LinkUtil.getDocumentLink(IOUtils.toByteArray(payloadStream.openStream()));
                DocumentBucketPath sourceDocumentBucketPath = documentLink.getSourceDocumentBucketPath();
                payloadStream = documentPersistenceService.loadDocumentStream(keyStoreAccess, sourceDocumentBucketPath);
                LOGGER.info("finished load link " + documentFQN);
            }
            LOGGER.info("finished readDocumentStream for " + userIDAuth + " " + documentFQN);
            return new DSDocumentStream(documentFQN, payloadStream.openStream(), new DSDocumentMetaInfo(payloadStream.getStorageMetadata().getUserMetadata()));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public void deleteDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        checkUserKeyPassword(userIDAuth);
        DocumentBucketPath documentBucketPath = getTheDocumentBucketPath(userIDAuth.getUserID(), documentFQN);
        bucketService.deletePlainFile(documentBucketPath);
    }

    /**
     * GRANT/DOCUMENT
     * ===========================================================================================
     */
    @Override
    public void grantAccessToUserForFolder(UserIDAuth userIDAuth, UserID receiverUserID,
                                           DocumentDirectoryFQN documentDirectoryFQN,
                                           AccessType accessType) {
        LOGGER.info("start grant access for " + userIDAuth + " to  " + receiverUserID + " for " + documentDirectoryFQN + " with " + accessType);

        {
            BucketDirectory userRootBucketDirectory = UserIDUtil.getUserRootBucketDirectory(userIDAuth.getUserID());
            if (!bucketService.bucketExists(userRootBucketDirectory)) {
                throw new UserIDDoesNotExistException(userIDAuth.getUserID().toString());
            }
        }
        {
            BucketDirectory userRootBucketDirectory = UserIDUtil.getUserRootBucketDirectory(receiverUserID);
            if (!bucketService.bucketExists(userRootBucketDirectory)) {
                throw new UserIDDoesNotExistException(receiverUserID.toString());
            }
        }

        BucketDirectory homeBucketDirectory = UserIDUtil.getHomeBucketDirectory(userIDAuth.getUserID());
        BucketDirectory documentBucketDirectory = homeBucketDirectory.append(new BucketDirectory(documentDirectoryFQN.getValue()));

        AccessType grantedAccess = GrantUtil.getAccessTypeOfBucketGrantFile(bucketService, documentBucketDirectory, userIDAuth.getUserID(), receiverUserID);
        if (grantedAccess.equals(accessType)) {
            LOGGER.debug("nothing to do. granted access already exists for " + userIDAuth + " to  " + receiverUserID + " for " + documentDirectoryFQN + " with " + accessType);
            return;
        }
        if (!grantedAccess.equals(AccessType.NONE)) {
            LOGGER.debug("granted access for " + userIDAuth + " to  " + receiverUserID + " for " + documentDirectoryFQN + " will be changed from " + grantedAccess + " to " + accessType);
        }

        DocumentKeyIDWithKeyAndAccessType usersDocumentKeyIDWithKeyAndAccessType = getOrCreateDocumentKeyIDwithKeyForBucketPath(userIDAuth, documentBucketDirectory, AccessType.WRITE);
        {
            DocumentKeyIDWithKeyAndAccessType receiversDocumentKeyWithIDAndAccessType = new DocumentKeyIDWithKeyAndAccessType(usersDocumentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey(), accessType);
            UserIDAuth receiverUserIDAuth = new UserIDAuth(receiverUserID, null);
            KeyStoreAccess receiverKeyStoreAccess = getKeyStoreAccess(receiverUserIDAuth);
            if (AccessType.NONE.equals(accessType)) {
                deleteGuardForBucket(receiverKeyStoreAccess, receiversDocumentKeyWithIDAndAccessType, documentBucketDirectory);
            } else {
                createAsymmetricGuardForBucket(receiverKeyStoreAccess, receiversDocumentKeyWithIDAndAccessType, documentBucketDirectory, OverwriteFlag.TRUE);
            }
        }

        {
            // create a grant file, so we know, who received a grant for what
            GrantUtil.saveBucketGrantFile(bucketService, documentBucketDirectory, userIDAuth.getUserID(), receiverUserID, accessType);
        }

        LOGGER.info("finished grant access for " + userIDAuth + " to  " + receiverUserID + " for " + documentDirectoryFQN + " with " + accessType);
    }

    @Override
    public void storeGrantedDocument(UserIDAuth userIDAuth, UserID documentOwner, DSDocument dsDocument) {
        LOGGER.info("start storeDocument for " + userIDAuth + " " + documentOwner + " " + dsDocument.getDocumentFQN());

        SimpleStorageMetadataImpl storageMetadata = new SimpleStorageMetadataImpl();
        storageMetadata.setSize(new Long(dsDocument.getDocumentContent().getValue().length));
        DocumentBucketPath documentBucketPath = getTheDocumentBucketPath(documentOwner, dsDocument.getDocumentFQN());
        DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType = getDocumentKeyIDwithKeyForBucketPath(userIDAuth, documentBucketPath.getBucketDirectory());
        if (!documentKeyIDWithKeyAndAccessType.getAccessType().equals(AccessType.WRITE)) {
            throw new NoWriteAccessException(userIDAuth.getUserID(), documentOwner, dsDocument.getDocumentFQN());
        }
        documentPersistenceService.persistDocument(
                documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey(),
                documentBucketPath,
                OverwriteFlag.TRUE,
                new SimplePayloadImpl(storageMetadata, dsDocument.getDocumentContent().getValue()));
        LOGGER.info("finished storeDocument for " + userIDAuth + " " + documentOwner + " " + dsDocument.getDocumentFQN());
    }


    @Override
    public DSDocument readGrantedDocument(UserIDAuth userIDAuth, UserID documentOwner, DocumentFQN documentFQN) {
        LOGGER.info("start readDocument for " + userIDAuth + " " + documentOwner + " " + documentFQN);
        DocumentBucketPath documentBucketPath = getTheDocumentBucketPath(documentOwner, documentFQN);
        KeyStoreAccess keyStoreAccess = getKeyStoreAccess(userIDAuth);
        Payload payload = documentPersistenceService.loadDocument(keyStoreAccess, documentBucketPath);
        UserMetaData userMetaData = payload.getStorageMetadata().getUserMetadata();
        if (userMetaData.find(LINK_KEY) != null) {
            LOGGER.info("start load link " + documentFQN);
            DocumentLink documentLink = LinkUtil.getDocumentLink(payload.getData());
            DocumentBucketPath sourceDocumentBucketPath = documentLink.getSourceDocumentBucketPath();
            payload = documentPersistenceService.loadDocument(keyStoreAccess, sourceDocumentBucketPath);
            LOGGER.info("finished load link " + documentFQN);
        }
        LOGGER.info("finisherd readDocument for " + userIDAuth + " " + documentOwner + " " + documentFQN);
        return new DSDocument(documentFQN, new DocumentContent(payload.getData()), new DSDocumentMetaInfo(payload.getStorageMetadata().getUserMetadata()));
    }



    @Override
    public void linkDocument(UserIDAuth userIDAuth, DocumentFQN sourceDocumentFQN, DocumentFQN destinationDocumentFQN) {
        LOGGER.info("start linkDocument for " + userIDAuth + " " + sourceDocumentFQN + " -> " + destinationDocumentFQN);

        // Wir prüfen lediglich, ob es den source Bucket gibt und ob wir darauf Zugriff haben.
        // Ob das Document selbset existiert, bleibt vorher ein Geheimnis
        DocumentBucketPath sourceDocumentBucketPath = getTheDocumentBucketPath(userIDAuth.getUserID(), sourceDocumentFQN);
        DocumentKeyIDWithKeyAndAccessType sourceDocumentKeyIDWithKeyAndAccessType = getDocumentKeyIDwithKeyForBucketPath(userIDAuth, sourceDocumentBucketPath.getBucketDirectory());

        DocumentBucketPath destinationDocumentBucketPath = getTheDocumentBucketPath(userIDAuth.getUserID(), destinationDocumentFQN);
        DocumentKeyIDWithKeyAndAccessType destinationDocumentKeyIDWithKeyAndAccessType = getOrCreateDocumentKeyIDwithKeyForBucketPath(userIDAuth, destinationDocumentBucketPath.getBucketDirectory(), AccessType.WRITE);

        // TODO, die keys der destination müssen noch in das linkDocument (das faktisch ein guard ist)
        DocumentLink documentLink = new DocumentLink(sourceDocumentBucketPath, destinationDocumentBucketPath);
        DocumentLinkAsDSDocument dsDocumentLink = LinkUtil.createDSDocument(documentLink, destinationDocumentFQN);

        storeDocument(userIDAuth, dsDocumentLink);
        LOGGER.info("finished linkDocument for " + userIDAuth + " " + sourceDocumentFQN + " -> " + destinationDocumentFQN);
    }



    private DocumentKeyID createAsymmetricGuardForBucket(KeyStoreAccess keyStoreAccess,
                                                         DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType,
                                                         BucketDirectory documentDirectory,
                                                         OverwriteFlag overwriteFlag) {
        LOGGER.debug("start create asymmetric guard for " + documentDirectory + " " + keyStoreAccess.getKeyStorePath().getBucketDirectory());
        documentGuardService.createDocumentGuardFor(GuardKeyType.PUBLIC_KEY, keyStoreAccess, documentKeyIDWithKeyAndAccessType, overwriteFlag);
        GuardUtil.saveBucketGuardKeyFile(bucketService, keyStoreAccess.getKeyStorePath().getBucketDirectory(), documentDirectory, documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey().getDocumentKeyID());
        LOGGER.debug("finished create asymmetric guard for " + documentDirectory + " " + keyStoreAccess.getKeyStorePath().getBucketDirectory());
        return documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey().getDocumentKeyID();
    }

    private void deleteGuardForBucket(KeyStoreAccess keyStoreAccess,
                                      DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType,
                                      BucketDirectory documentDirectory
    ) {
        LOGGER.debug("start delete guard for " + documentDirectory);
        BucketPath documentGuardFileBucketPath = DocumentGuardLocation.getBucketPathOfGuard(keyStoreAccess.getKeyStorePath(),
                documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey().getDocumentKeyID());
        bucketService.deletePlainFile(documentGuardFileBucketPath);

        GuardUtil.deleteBucketGuardKeyFile(bucketService, keyStoreAccess.getKeyStorePath().getBucketDirectory(), documentDirectory);
        LOGGER.debug("finished delete guard for " + documentDirectory);
    }

    private KeyStoreAccess getKeyStoreAccess(UserIDAuth userIDAuth) {
        BucketPath keyStorePath = UserIDUtil.getKeyStorePath(userIDAuth.getUserID());
        KeyStoreAuth keyStoreAuth = UserIDUtil.getKeyStoreAuth(userIDAuth);
        KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStorePath, keyStoreAuth);
        return keyStoreAccess;
    }

    private DocumentBucketPath getTheDocumentBucketPath(UserID userID, DocumentFQN documentFQN) {
        return new DocumentBucketPath(UserIDUtil.getHomeBucketDirectory(userID).appendName(documentFQN.getValue()));
    }

    private DSDocument createWelcomeDocument() {
        String text = "Welcome to the DocumentStore";
        DocumentContent documentContent = new DocumentContent(text.getBytes());
        DocumentFQN documentFQN = new DocumentFQN("README.txt");
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent, null);
        return dsDocument;
    }


    /**
     * Es wird extra nur die KeyID zurückgegeben. Damit der Zugriff auf den Key wirklich über den
     * KeyStore erfolgt und damit dann auch getestet ist.
     */
    private DocumentKeyID createGuardForBucket(KeyStoreAccess keyStoreAccess, BucketDirectory documentDirectory, AccessType accessType) {
        LOGGER.debug("start create new guard for " + documentDirectory);
        DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType = new DocumentKeyIDWithKeyAndAccessType(documentGuardService.createDocumentKeyIdWithKey(), accessType);
        documentGuardService.createDocumentGuardFor(GuardKeyType.SECRET_KEY, keyStoreAccess, documentKeyIDWithKeyAndAccessType, OverwriteFlag.FALSE);
        GuardUtil.saveBucketGuardKeyFile(bucketService,
                keyStoreAccess.getKeyStorePath().getBucketDirectory(),
                documentDirectory, documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey().getDocumentKeyID());
        LOGGER.debug("finished create new guard for " + documentDirectory);
        return documentKeyIDWithKeyAndAccessType.getDocumentKeyIDWithKey().getDocumentKeyID();
    }

    private DocumentKeyIDWithKeyAndAccessType getOrCreateDocumentKeyIDwithKeyForBucketPath(UserIDAuth userIDAuth,
                                                                                           BucketDirectory documentDirectory,
                                                                                           AccessType accessType) {
        LOGGER.debug("search key for " + documentDirectory);
        KeyStoreAccess keyStoreAccess = getKeyStoreAccess(userIDAuth);
        DocumentKeyID documentKeyID = GuardUtil.tryToLoadBucketGuardKeyFile(bucketService, keyStoreAccess.getKeyStorePath().getBucketDirectory(), documentDirectory);
        if (documentKeyID == null) {
            documentKeyID = createGuardForBucket(keyStoreAccess, documentDirectory, accessType);
        }
        DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType = documentGuardService.loadDocumentKeyIDWithKeyAndAccessTypeFromDocumentGuard(keyStoreAccess, documentKeyID);
        LOGGER.debug("found " + documentKeyIDWithKeyAndAccessType + " for " + documentDirectory);
        return documentKeyIDWithKeyAndAccessType;
    }

    private DocumentKeyIDWithKeyAndAccessType getDocumentKeyIDwithKeyForBucketPath(UserIDAuth userIDAuth, BucketDirectory documentDirectory) {
        LOGGER.debug("get key for " + documentDirectory);
        KeyStoreAccess keyStoreAccess = getKeyStoreAccess(userIDAuth);
        DocumentKeyID documentKeyID = GuardUtil.loadBucketGuardKeyFile(bucketService, keyStoreAccess.getKeyStorePath().getBucketDirectory(), documentDirectory);
        DocumentKeyIDWithKeyAndAccessType documentKeyIDWithKeyAndAccessType = documentGuardService.loadDocumentKeyIDWithKeyAndAccessTypeFromDocumentGuard(keyStoreAccess, documentKeyID);
        LOGGER.debug("found " + documentKeyIDWithKeyAndAccessType + " for " + documentDirectory);
        return documentKeyIDWithKeyAndAccessType;
    }

    private void checkUserKeyPassword(UserIDAuth userIDAuth) {
        LOGGER.warn("ACHTUNG, ES WIRD NICHT GEPRÜFT, OB DER BENUTZER " + userIDAuth.getUserID() + " AUCH DAS KORREKTE PASSWORD BENUTZT");
    }


}
