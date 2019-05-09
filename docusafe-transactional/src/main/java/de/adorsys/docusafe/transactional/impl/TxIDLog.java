package de.adorsys.docusafe.transactional.impl;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.impl.helper.*;
import de.adorsys.docusafe.transactional.types.TxID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Created by peter on 11.06.18 at 15:48.
 */
public class TxIDLog {
    public final static int MAX_COMMITED_TX_FOR_CLEANUP = 5;
    private final static Logger LOGGER = LoggerFactory.getLogger(TxIDLog.class);
    private static String LOG_FILE_NAME = "LastCommitedTxID.txt";
    private static DocumentFQN txidLogFilename = TransactionalDocumentSafeServiceImpl.txMeta.addName(LOG_FILE_NAME);

    private TransactionInformationList txidList = new TransactionInformationList();

    public static TxID findLastCommitedTxID(DocumentSafeService documentSafeService, UserIDAuth userIDAuth) {
        if (documentSafeService.documentExists(userIDAuth, txidLogFilename)) {
            DSDocument dsDocument = documentSafeService.readDocument(userIDAuth, txidLogFilename);
            TxIDLog txIDLog = new Class2JsonHelper().txidLogFromContent(dsDocument.getDocumentContent());
            if (txIDLog.txidList.isEmpty()) {
                throw new BaseException("file " + txidLogFilename + " must not be empty");
            }
            int size = txIDLog.txidList.size();
            TransactionInformation lastTuple = txIDLog.txidList.get(size - 1);
            return lastTuple.getCurrentTxID();
        }
        return null;
    }

    public static void saveJustFinishedTx(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, CurrentTransactionData currentTransactionData) {

        // we synchonize not all methods, but those, refering to the same user
        // very important the String.intern() call, otherwise Strings with the same value my be different objects!
        synchronized (userIDAuth.getUserID().getValue().intern()) {
            LOGGER.debug("start synchronized for " + userIDAuth.getUserID().getValue());
            TxIDHashMapWrapper joinedTx = null;
            TxIDLog txIDLog = new TxIDLog();
            if (documentSafeService.documentExists(userIDAuth, txidLogFilename)) {
                DSDocument dsDocument = documentSafeService.readDocument(userIDAuth, txidLogFilename);
                txIDLog = new Class2JsonHelper().txidLogFromContent(dsDocument.getDocumentContent());
            }
            if (!txIDLog.txidList.isEmpty()) {
                TransactionInformation lastTuple = txIDLog.txidList.get(txIDLog.txidList.size() - 1);
                TxID lastCommitedTxID = lastTuple.getCurrentTxID();

                // now read file of lastCommittedTx
                TxID previousTxID = currentTransactionData.getCurrentTxHashMap().getLastCommitedTxID();
                if (!lastCommitedTxID.equals(previousTxID)) {
                    TxIDHashMapWrapper stateOfLastCommitedTx = TxIDHashMapWrapper.readHashMapOfTx(documentSafeService, userIDAuth, lastCommitedTxID);
                    joinedTx = new ParallelTransactionLogic().join(stateOfLastCommitedTx, currentTransactionData.getInitialTxHashMap(), currentTransactionData.getCurrentTxHashMap(), currentTransactionData.getDocumentsReadInThisTx());
                    joinedTx.saveOnce(documentSafeService, userIDAuth);
                }
            }

            {
                TxID previousTxID = currentTransactionData.getCurrentTxHashMap().getLastCommitedTxID();
                TxID currentTxID = currentTransactionData.getCurrentTxHashMap().getCurrentTxID();
                Date start = currentTransactionData.getCurrentTxHashMap().getBeginTx();
                Date finished = currentTransactionData.getCurrentTxHashMap().getEndTx();
                txIDLog.txidList.add(new TransactionInformation(start, finished, previousTxID, currentTxID));
            }
            if (joinedTx != null) {
                TxID previousTxID = joinedTx.getLastCommitedTxID();
                TxID currentTxID = joinedTx.getCurrentTxID();
                Date start = joinedTx.getBeginTx();
                Date finished = joinedTx.getEndTx();
                txIDLog.txidList.add(new TransactionInformation(start, finished, previousTxID, currentTxID));
            }
            DSDocument document = new DSDocument(txidLogFilename, new Class2JsonHelper().txidLogToContent(txIDLog));
            documentSafeService.storeDocument(userIDAuth, document);
            LOGGER.debug("successfully wrote new Version to " + txidLogFilename);

            // as we are here in a synchronized space, we here do the cleanup
            int size = txIDLog.txidList.size();
            if (size > MAX_COMMITED_TX_FOR_CLEANUP) {
                txIDLog.txidList = CleanupLogic.cleaupTxHistory(documentSafeService, userIDAuth, txIDLog.txidList);
                int newsize = txIDLog.txidList.size();
                document = new DSDocument(txidLogFilename, new Class2JsonHelper().txidLogToContent(txIDLog));
                documentSafeService.storeDocument(userIDAuth, document);
                LOGGER.info("cleanup reduced number of tx from " + size + " to " + newsize + " in " + txidLogFilename);
            }
            LOGGER.debug("finished synchronized for " + userIDAuth.getUserID().getValue());
        }
    }
}
