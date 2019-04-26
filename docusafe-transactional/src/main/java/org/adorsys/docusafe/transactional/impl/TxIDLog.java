package org.adorsys.docusafe.transactional.impl;

import de.adorsys.common.exceptions.BaseException;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.adorsys.docusafe.transactional.impl.helper.*;
import org.adorsys.docusafe.transactional.types.TxID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Created by peter on 11.06.18 at 15:48.
 */
public class TxIDLog {
    private final static int MAX_COMMITED_TX_FOR_CLEANUP = 5;
    private final static Logger LOGGER = LoggerFactory.getLogger(TxIDLog.class);
    private static String LOG_FILE_NAME = "LastCommitedTxID.txt";
    private static DocumentFQN txidLogFilename = TransactionalDocumentSafeServiceImpl.txMeta.addName(LOG_FILE_NAME);


    private TransactionInformationList txidList = new TransactionInformationList();

    public static LastCommitedTxID findLastCommitedTxID(DocumentSafeService documentSafeService, UserIDAuth userIDAuth) {
        if (documentSafeService.documentExists(userIDAuth, txidLogFilename)) {
            DSDocument dsDocument = documentSafeService.readDocument(userIDAuth, txidLogFilename);
            TxIDLog txIDLog = new Class2JsonHelper().txidLogFromContent(dsDocument.getDocumentContent());
            if (txIDLog.txidList.isEmpty()) {
                throw new BaseException("file " + txidLogFilename + " must not be empty");
            }
            int size = txIDLog.txidList.size();
            if (size > MAX_COMMITED_TX_FOR_CLEANUP) {
                txIDLog.txidList = CleanupLogic.cleaupTxHistory(documentSafeService, userIDAuth, txIDLog.txidList);
                size = txIDLog.txidList.size();
                DSDocument document = new DSDocument(txidLogFilename, new Class2JsonHelper().txidLogToContent(txIDLog));
                documentSafeService.storeDocument(userIDAuth, document);
            }
            TransactionInformation lastTuple = txIDLog.txidList.get(size - 1);
            return new LastCommitedTxID(lastTuple.getCurrentTxID().getValue());
        }
        return null;
    }

    public static void saveJustFinishedTx(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, CurrentTransactionData currentTransactionData) {
        // we synchonize not all methods, but those, refering to the same user


        synchronized (userIDAuth.getUserID().getValue()) {
            TxIDHashMapWrapper joinedTx = null;
            TxIDLog txIDLog = new TxIDLog();
            if (documentSafeService.documentExists(userIDAuth, txidLogFilename)) {
                DSDocument dsDocument = documentSafeService.readDocument(userIDAuth, txidLogFilename);
                txIDLog = new Class2JsonHelper().txidLogFromContent(dsDocument.getDocumentContent());
            }
            if (!txIDLog.txidList.isEmpty()) {
                TransactionInformation lastTuple = txIDLog.txidList.get(txIDLog.txidList.size() - 1);
                LastCommitedTxID lastCommitedTxID = new LastCommitedTxID(lastTuple.getCurrentTxID().getValue());

                // now read file of lastCommittedTx
                LastCommitedTxID previousTxID = currentTransactionData.getCurrentTxHashMap().getLastCommitedTxID();
                if (!lastCommitedTxID.equals(previousTxID)) {
                    TxIDHashMapWrapper stateOfLastCommitedTx = TxIDHashMapWrapper.readHashMapOfTx(documentSafeService, userIDAuth, lastCommitedTxID);
                    joinedTx = new ParallelTransactionLogic().join(stateOfLastCommitedTx, currentTransactionData.getInitialTxHashMap(), currentTransactionData.getCurrentTxHashMap(), currentTransactionData.getDocumentsReadInThisTx());
                    joinedTx.saveOnce(documentSafeService, userIDAuth);
                }
            }

            {
                LastCommitedTxID previousTxID = currentTransactionData.getCurrentTxHashMap().getLastCommitedTxID();
                TxID currentTxID = currentTransactionData.getCurrentTxHashMap().getCurrentTxID();
                Date start = currentTransactionData.getCurrentTxHashMap().getBeginTx();
                Date finished = currentTransactionData.getCurrentTxHashMap().getEndTx();
                txIDLog.txidList.add(new TransactionInformation(start, finished, previousTxID, currentTxID));
            }
            if (joinedTx != null) {
                LastCommitedTxID previousTxID = joinedTx.getLastCommitedTxID();
                TxID currentTxID = joinedTx.getCurrentTxID();
                Date start = joinedTx.getBeginTx();
                Date finished = joinedTx.getEndTx();
                txIDLog.txidList.add(new TransactionInformation(start, finished, previousTxID, currentTxID));
            }
            DSDocument document = new DSDocument(txidLogFilename, new Class2JsonHelper().txidLogToContent(txIDLog));
            documentSafeService.storeDocument(userIDAuth, document);
            LOGGER.debug("successfully wrote new Version to " + txidLogFilename);
        }
    }


}
