package org.adorsys.docusafe.transactional.impl;

import de.adorsys.common.exceptions.BaseException;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.MoveType;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.adorsys.docusafe.transactional.RequestMemoryContext;
import org.adorsys.docusafe.transactional.TransactionalDocumentSafeService;
import org.adorsys.docusafe.transactional.exceptions.TxInnerException;
import org.adorsys.docusafe.transactional.exceptions.TxNotActiveException;
import org.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import org.adorsys.docusafe.transactional.types.TxDocumentFQNVersion;
import org.adorsys.docusafe.transactional.types.TxID;
import de.adorsys.dfs.connection.api.filesystem.exceptions.FileNotFoundException;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Created by peter on 11.06.18 at 15:01.
 */
public class TransactionalDocumentSafeServiceImpl extends NonTransactionalDocumentSafeServiceImpl implements TransactionalDocumentSafeService {
    private final static Logger LOGGER = LoggerFactory.getLogger(TransactionalDocumentSafeServiceImpl.class);
    final static DocumentDirectoryFQN txMeta = new DocumentDirectoryFQN("meta.tx");
    final static DocumentDirectoryFQN txContent = new DocumentDirectoryFQN("tx");
    private RequestMemoryContext requestMemoryContext;

    public static final String CURRENT_TRANSACTION_DATA = "CurrentTransactionData";

    public TransactionalDocumentSafeServiceImpl(RequestMemoryContext requestMemoryContext, DocumentSafeService documentSafeService) {
        super(documentSafeService);
        this.requestMemoryContext = requestMemoryContext;
        LOGGER.debug("new Instance of TransactionalDocumentSafeServiceImpl");
    }

    // ============================================================================================
    // TRANSACTIONAL
    // ============================================================================================

    @Override
    public void beginTransaction(UserIDAuth userIDAuth) {
        Date beginTxDate = new Date();
        CurrentTransactionData currentTransactionData = findCurrentTransactionData(userIDAuth.getUserID());
        if (currentTransactionData != null) {
            throw new TxInnerException();
        }

        TxID currentTxID = new TxID();
        LOGGER.debug("beginTransaction " + currentTxID.getValue());
        TxIDHashMapWrapper txIDHashMapWrapper = TxIDHashMapWrapper.fromPreviousFileOrNew(documentSafeService, userIDAuth, currentTxID, beginTxDate);
        currentTransactionData = new CurrentTransactionData(currentTxID, txIDHashMapWrapper);
        setCurrentTransactionData(userIDAuth.getUserID(), currentTransactionData);
    }

    @Override
    public void txStoreDocument(UserIDAuth userIDAuth, DSDocument dsDocument) {
        LOGGER.debug("txStoreDocument " + dsDocument.getDocumentFQN().getValue() + " " + getCurrentTxID(userIDAuth.getUserID()));
        documentSafeService.storeDocument(userIDAuth, modifyTxDocument(dsDocument, getCurrentTxID(userIDAuth.getUserID())));
        getCurrentTxIDHashMap(userIDAuth.getUserID()).storeDocument(dsDocument.getDocumentFQN());
    }

    @Override
    public DSDocument txReadDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        LOGGER.debug("txReadDocument " + documentFQN.getValue() + " " + getCurrentTxID(userIDAuth.getUserID()));
        TxID txidOfDocument = getCurrentTxIDHashMap(userIDAuth.getUserID()).getTxIDOfDocument(documentFQN);
        DSDocument dsDocument = documentSafeService.readDocument(userIDAuth, modifyTxDocumentName(documentFQN, txidOfDocument));
        markDocumentRead(userIDAuth, documentFQN, txidOfDocument);
        return new DSDocument(documentFQN, dsDocument.getDocumentContent());
    }

    @Override
    public void txDeleteDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        LOGGER.debug("txDeleteDocument " + documentFQN.getValue() + " " + getCurrentTxID(userIDAuth.getUserID()));
        getCurrentTxIDHashMap(userIDAuth.getUserID()).deleteDocument(documentFQN);
    }

    @Override
    public TxBucketContentFQN txListDocuments(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
        LOGGER.debug("txListDocuments " + getCurrentTxID(userIDAuth.getUserID()));
        TxIDHashMapWrapper txIDHashMapWrapper = getCurrentTxIDHashMap(userIDAuth.getUserID());
        return txIDHashMapWrapper.list(documentDirectoryFQN, recursiveFlag);
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
        LOGGER.debug("txDocumentExists " + documentFQN.getValue() + " " + getCurrentTxID(userIDAuth.getUserID()));
        return getCurrentTxIDHashMap(userIDAuth.getUserID()).documentExists(documentFQN);
    }

    @Override
    public void txDeleteFolder(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN) {
        LOGGER.debug("txDeleteFolder " + documentDirectoryFQN.getValue() + " " + getCurrentTxID(userIDAuth.getUserID()));
        getCurrentTxIDHashMap(userIDAuth.getUserID()).deleteFolder(documentDirectoryFQN);
    }

    @Override
    public void endTransaction(UserIDAuth userIDAuth) {
        try {
            TxID txid = getCurrentTxID(userIDAuth.getUserID());
            LOGGER.debug("endTransaction " + txid.getValue());
            CurrentTransactionData currentTransactionData = getCurrentTransactionData(userIDAuth.getUserID());
            boolean changed = getCurrentTransactionData(userIDAuth.getUserID()).anyDifferenceToInitalState();
            if (changed) {
                LOGGER.info("something has changed, so write down the new state");
                TxIDHashMapWrapper txIDHashMapWrapper = getCurrentTxIDHashMap(userIDAuth.getUserID());
                txIDHashMapWrapper.setEndTransactionDate(new Date());
                txIDHashMapWrapper.saveOnce(documentSafeService, userIDAuth);
                txIDHashMapWrapper.transactionIsOver(documentSafeService, userIDAuth, currentTransactionData);
                for (DocumentFQN doc : getCurrentTransactionData(userIDAuth.getUserID()).getNonTxInboxDocumentsToBeDeletedAfterCommit()) {
                    try {
                        LOGGER.debug("delete file of inbox after commit " + doc);
                        documentSafeService.deleteDocumentFromInbox(userIDAuth, doc);
                    } catch (BaseException e) {
                        LOGGER.warn("Exception is ignored. File deletion after commit does not raise exception");
                    } catch (Exception e) {
                        new BaseException(e);
                        LOGGER.warn("Exception is ignored. File deletion after commit does not raise exception");
                    }
                }
            } else {
                LOGGER.info("nothing has changed, so nothing has to be written down");
            }
        }
        finally {
            setCurrentTransactionDataToNull(userIDAuth.getUserID());
        }
    }

    // ============================================================================================
    // INBOX STUFF
    // ============================================================================================
    @Override
    @SuppressWarnings("Duplicates")
    public void txMoveDocumentToInboxOfUser(UserIDAuth userIDAuth, UserID receiverUserID, DocumentFQN sourceDocumentFQN, DocumentFQN destDocumentFQN, MoveType moveType) {
        // kopiert aus der TransactionalDocumenetSafeServiceImpl
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
    public DSDocument txMoveDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source, DocumentFQN destination) {
        // kopiert aus der TransactionalDocumenetSafeServiceImpl
        LOGGER.debug("start nonTxReadFromInbox for " + userIDAuth +  " " + source + " to " + destination);

        // Hier kann die Methode des documentSafeService nicht benutzt werden, da es nicht im Transaktionskontext geschieht
        // Also muss das Document hier von Hand aus der Inbox geholt und gespeichert werden.

        // Holen des Documentens, aber ändern des Pfades
        DSDocument documentFromInbox = documentSafeService.readDocumentFromInbox(userIDAuth, source);
        DSDocument dsDocument = new DSDocument(destination, documentFromInbox.getDocumentContent());

        // Speichern des Documents
        txStoreDocument(userIDAuth, dsDocument);

        // Merken, dass es aus der Inbox nach dem Commit gelöscht werden muss
        getCurrentTransactionData(userIDAuth.getUserID()).addNonTxInboxFileToBeDeletedAfterCommit(source);

        LOGGER.debug("finishdd nonTxReadFromInbox for " + userIDAuth +  " " + source + " to " + destination);
        // Anstatt das locale Object zurückzugeben rufen wir die richtige Methode auf, die es ja nur aus Map lesen sollte.
        return txReadDocument(userIDAuth, destination);
    }


    // ============================================================================================
    // PRIVATE STUFF
    // ============================================================================================

    public static DSDocument modifyTxDocument(DSDocument dsDocument, TxID txid) {
        return new DSDocument(
                modifyTxDocumentName(dsDocument.getDocumentFQN(), txid),
                dsDocument.getDocumentContent());
    }

    public static DocumentFQN modifyTxDocumentName(DocumentFQN origName, TxID txid) {
        return txContent.addName(origName.getValue() + "." + txid.getValue());
    }

    public static DocumentFQN modifyTxMetaDocumentName(DocumentFQN origName, TxID txid) {
        return txMeta.addName(origName.getValue() + "." + txid.getValue());
    }

    private TxID getCurrentTxID(UserID userID) {
        return getCurrentTransactionData(userID).getCurrentTxID();
    }

    private void markDocumentRead(UserIDAuth userIDAuth, DocumentFQN documentFQN, TxID txidOfDocument) {
        getCurrentTransactionData(userIDAuth.getUserID()).getDocumentsReadInThisTx().put(documentFQN, txidOfDocument);
    }

    private TxIDHashMapWrapper getCurrentTxIDHashMap(UserID userID) {
        return getCurrentTransactionData(userID).getCurrentTxHashMap();
    }

    private void setCurrentTransactionDataToNull(UserID userID) {
        setCurrentTransactionData(userID, null);
    }

    private CurrentTransactionData findCurrentTransactionData(UserID userID) {
        return (CurrentTransactionData) requestMemoryContext.get(CURRENT_TRANSACTION_DATA + "-" + userID.getValue());
    }

    private CurrentTransactionData getCurrentTransactionData(UserID userID) {
        CurrentTransactionData currentTransactionData = findCurrentTransactionData(userID);
        if (currentTransactionData == null) {
            throw new TxNotActiveException(userID);
        }
        return currentTransactionData;
    }

    private void setCurrentTransactionData(UserID userID, CurrentTransactionData currentTransactionData) {
        requestMemoryContext.put(CURRENT_TRANSACTION_DATA  + "-" + userID.getValue(), currentTransactionData);
    }

}
