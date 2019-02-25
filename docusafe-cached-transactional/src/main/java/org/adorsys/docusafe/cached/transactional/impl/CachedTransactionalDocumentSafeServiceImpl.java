package org.adorsys.docusafe.cached.transactional.impl;

import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.MoveType;
import org.adorsys.docusafe.business.types.UserID;
import org.adorsys.docusafe.business.types.complex.BucketContentFQNWithUserMetaData;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DSDocumentMetaInfo;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.business.types.complex.UserIDAuth;
import org.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import org.adorsys.docusafe.cached.transactional.exceptions.CacheException;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.adorsys.docusafe.transactional.RequestMemoryContext;
import org.adorsys.docusafe.transactional.TransactionalDocumentSafeService;
import org.adorsys.docusafe.transactional.exceptions.TxNotActiveException;
import org.adorsys.docusafe.transactional.impl.CurrentTransactionData;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import org.adorsys.docusafe.transactional.types.TxDocumentFQNVersion;
import org.adorsys.docusafe.transactional.types.TxID;
import org.adorsys.encobject.domain.Payload;
import org.adorsys.encobject.filesystem.exceptions.FileNotFoundException;
import org.adorsys.encobject.types.ListRecursiveFlag;
import org.adorsys.encobject.types.OverwriteFlag;
import org.adorsys.encobject.types.PublicKeyJWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 21.06.18 at 11:51.
 * <p>
 * Es gibt drei Listen:
 * mapToStore enthält alle Dokumente, die gespeichert werden sollen.
 * Es wird nicht gepüft, ob sich der Inhalt gehändert hat, oder nicht.
 * <p>
 * mapToRead enthält alle Documente, die gelesen wurden. Wenn diese anschliessend
 * gespeichert werden, dann sind sie zusätzlich in mapToStore.
 * <p>
 * setToDelete enhält alle Namen der Dokumente, die gelöscht werden sollen.
 * Der name darf dann nicht in mapToRead oder mapToStore auftauchen.
 */
public class CachedTransactionalDocumentSafeServiceImpl implements CachedTransactionalDocumentSafeService {
    private final static Logger LOGGER = LoggerFactory.getLogger(CachedTransactionalDocumentSafeServiceImpl.class);
    public static final String CACHEND_TRANSACTIONAL_CONTEXT_MAP = "cachendTransactionalContextMap";
    private TransactionalDocumentSafeService transactionalDocumentSafeService;
    private RequestMemoryContext requestMemoryContext;
    private DocumentSafeService documentSafeService;

    public CachedTransactionalDocumentSafeServiceImpl(RequestMemoryContext requestMemoryContext, TransactionalDocumentSafeService transactionalDocumentSafeService, DocumentSafeService documentSafeService) {
        this.transactionalDocumentSafeService = transactionalDocumentSafeService;
        this.documentSafeService =documentSafeService;
        this.requestMemoryContext = requestMemoryContext;
    }

    @Override
    public void createUser(UserIDAuth userIDAuth) {
        transactionalDocumentSafeService.createUser(userIDAuth);
    }

    @Override
    public void destroyUser(UserIDAuth userIDAuth) {
        transactionalDocumentSafeService.destroyUser(userIDAuth);
    }

    @Override
    public boolean userExists(UserID userID) {
        return transactionalDocumentSafeService.userExists(userID);
    }

    @Override
    public PublicKeyJWK findPublicEncryptionKey(UserID userID) {
        return transactionalDocumentSafeService.findPublicEncryptionKey(userID);
    }

    @Override
    public BucketContentFQNWithUserMetaData nonTxListInbox(UserIDAuth userIDAuth) {
        return transactionalDocumentSafeService.nonTxListInbox(userIDAuth);
    }

    @Override
    public void beginTransaction(UserIDAuth userIDAuth) {
        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        createTransactionalContext(getCurrentTxID(userIDAuth.getUserID()));
    }

    @Override
    public void txStoreDocument(UserIDAuth userIDAuth, DSDocument dsDocument) {
        getTransactionalContext(getCurrentTxID(userIDAuth.getUserID())).txStoreDocument(dsDocument);
    }

    @Override
    public DSDocument txReadDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return getTransactionalContext(getCurrentTxID(userIDAuth.getUserID())).txReadDocument(userIDAuth, documentFQN);
    }

    @Override
    public void txDeleteDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        getTransactionalContext(getCurrentTxID(userIDAuth.getUserID())).txDeleteDocument(documentFQN);
    }

    @Override
    public TxBucketContentFQN txListDocuments(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
        return getTransactionalContext(getCurrentTxID(userIDAuth.getUserID())).txListDocuments(userIDAuth, documentDirectoryFQN, recursiveFlag);
    }

    @Override
    public TxDocumentFQNVersion getVersion(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        TxBucketContentFQN txBucketContentFQN = txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.FALSE);
        if (txBucketContentFQN.getFilesWithVersion().isEmpty()) {
            throw new FileNotFoundException(documentFQN.getValue(), null);
        }
        return txBucketContentFQN.getFilesWithVersion().stream().findFirst().get().getVersion();
    }

    @Override
    public boolean txDocumentExists(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return getTransactionalContext(getCurrentTxID(userIDAuth.getUserID())).txDocumentExists(userIDAuth, documentFQN);
    }

    @Override
    public void txDeleteFolder(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN) {
        TxBucketContentFQN txBucketContentFQN = getTransactionalContext(getCurrentTxID(userIDAuth.getUserID())).txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE);
        txBucketContentFQN.getFiles().stream().forEach(documentFQN -> txDeleteDocument(userIDAuth, documentFQN));
    }

    @Override
    public void endTransaction(UserIDAuth userIDAuth) {
        TxID txid = getCurrentTxID(userIDAuth.getUserID());
        getTransactionalContext(txid).endTransaction(userIDAuth);
        deleteTransactionalContext(txid);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void txMoveDocumentToInboxOfUser(UserIDAuth userIDAuth, UserID receiverUserID, DocumentFQN sourceDocumentFQN, DocumentFQN destDocumentFQN, MoveType moveType) {
        LOGGER.debug("start txMoveDocumentToInboxOfUser from " + userIDAuth.getUserID() + " " + sourceDocumentFQN + " to " + receiverUserID + " " + destDocumentFQN);
        DSDocument document = txReadDocument(userIDAuth, sourceDocumentFQN);

        documentSafeService.writeDocumentToInboxOfUser(receiverUserID, document, destDocumentFQN);

        if (moveType.equals(MoveType.MOVE)) {
            txDeleteDocument(userIDAuth, sourceDocumentFQN);
        }
        LOGGER.debug("finished txMoveDocumentToInboxOfUser from " + userIDAuth.getUserID() + " " + sourceDocumentFQN + " to " + receiverUserID + " " + destDocumentFQN);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public DSDocument txMoveDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source, DocumentFQN destination, OverwriteFlag overwriteFlag) {
        LOGGER.debug("start nonTxReadFromInbox for " + userIDAuth +  " " + source + " to " + destination + " overwrite:" + overwriteFlag);

        // Hier kann die Methode des documentSafeService nicht benutzt werden, da es nicht im Transaktionskontext geschieht
        // Also muss das Document hier von Hand aus der Inbox geholt und gespeichert werden.

        // Holen des Documentes aus der Inbox mittels backdor Methode
        DSDocument dsDocumentFromInbox = documentSafeService.readDocumentFromInbox(userIDAuth, source);
        DSDocument dsDocument = new DSDocument(destination, dsDocumentFromInbox.getDocumentContent(), dsDocumentFromInbox.getDsDocumentMetaInfo());

        // Speichern des Documents
        txStoreDocument(userIDAuth, dsDocument);

        // Merken, dass es aus der Inbox nach dem Commit gelöscht werden muss
        getCurrentTransactionData(userIDAuth.getUserID()).addNonTxInboxFileToBeDeletedAfterCommit(source);

        LOGGER.debug("finishdd nonTxReadFromInbox for " + userIDAuth +  " " + source + " to " + destination + " overwrite:" + overwriteFlag);
        // Anstatt das locale Object zurückzugeben rufen wir die richtige Methode auf, die es ja nur aus Map lesen sollte.
        return txReadDocument(userIDAuth, destination);
    }

    private CachedTransactionalContext createTransactionalContext(TxID txid) {
        CachedTransactionalContextMap cachedTransactionalContextMap = (CachedTransactionalContextMap) requestMemoryContext.get(CACHEND_TRANSACTIONAL_CONTEXT_MAP);
        if (cachedTransactionalContextMap == null) {
            cachedTransactionalContextMap = new CachedTransactionalContextMap();
            requestMemoryContext.put(CACHEND_TRANSACTIONAL_CONTEXT_MAP, cachedTransactionalContextMap);
        }
        CachedTransactionalContext cachedTransactionalContext = new CachedTransactionalContext(transactionalDocumentSafeService, txid);
        cachedTransactionalContextMap.put(txid, cachedTransactionalContext);
        return cachedTransactionalContext;
    }

    private CachedTransactionalContext getTransactionalContext(TxID txid) {
        CachedTransactionalContextMap cachedTransactionalContextMap = (CachedTransactionalContextMap) requestMemoryContext.get(CACHEND_TRANSACTIONAL_CONTEXT_MAP);
        if (cachedTransactionalContextMap == null) {
            throw new CacheException("RequestContext has no CachedTransactionalContextMap. So Context for " + txid + " can not be searched");
        }
        CachedTransactionalContext cachedTransactionalContext = cachedTransactionalContextMap.get(txid);
        if (cachedTransactionalContext == null) {
            throw new CacheException("CachedTransactionalContextMap has no CachedContext for " + txid);
        }
        return cachedTransactionalContext;
    }

    private void deleteTransactionalContext(TxID txid) {
        CachedTransactionalContextMap cachedTransactionalContextMap = (CachedTransactionalContextMap) requestMemoryContext.get(CACHEND_TRANSACTIONAL_CONTEXT_MAP);
        if (cachedTransactionalContextMap == null) {
            throw new CacheException("RequestContext has no CachedTransactionalContextMap. So Context for " + txid + " can not be searched");
        }
        CachedTransactionalContext cachedTransactionalContext = cachedTransactionalContextMap.get(txid);
        if (cachedTransactionalContext == null) {
            throw new CacheException("CachedTransactionalContextMap has no CachedContext for " + txid);
        }
        LOGGER.debug("freeMemory() of tx " + txid);

        cachedTransactionalContext.freeMemory();
        cachedTransactionalContextMap.remove(txid);
    }

    public TxID getCurrentTxID(UserID userID) {
        CurrentTransactionData currentTransactionData = getCurrentTransactionData(userID);
        TxID txID = currentTransactionData.getCurrentTxID();
        if (txID == null) {
            throw new TxNotActiveException(userID);
        }
        return txID;
    }

    private CurrentTransactionData getCurrentTransactionData(UserID userID) {
        CurrentTransactionData currentTransactionData = (CurrentTransactionData) requestMemoryContext.get(TransactionalDocumentSafeServiceImpl.CURRENT_TRANSACTION_DATA + "-" + userID.getValue());
        if (currentTransactionData == null) {
            throw new TxNotActiveException(userID);
        }
        return currentTransactionData;
    }
}
