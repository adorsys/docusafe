package de.adorsys.docusafe.transactional.impl.helper;

import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import de.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import de.adorsys.docusafe.transactional.impl.TxIDHashMapWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public class CleanupLogic {
    private final static Logger LOGGER = LoggerFactory.getLogger(CleanupLogic.class);
    public static TransactionInformationList cleaupTxHistory(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, TransactionInformationList transactionInformationList) {
        return new CleanupLogic().cleanupTxHistory(documentSafeService, userIDAuth, transactionInformationList);
    }

    public TransactionInformationList cleanupTxHistory(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, TransactionInformationList transactionInformationList) {
        int size = transactionInformationList.size();
        if (size < 2) {
            return transactionInformationList;
        }
        LOGGER.debug("cleanup has to be done for " + (size - 1) + " previously commited transactions");

        // Find all files except the last tx
        HashSet<DocumentFQN> allPrevousFiles = new HashSet<>();
        {
            for (int i = 0; i < size - 1; i++) {
                TransactionInformation tuple = transactionInformationList.get(i);
                TxIDHashMapWrapper txIDHashMapWrapper = TxIDHashMapWrapper.readHashMapOfTx(documentSafeService, userIDAuth, new LastCommitedTxID(tuple.getCurrentTxID().getValue()));
                txIDHashMapWrapper.getMap().forEach((documentFQN, txID) -> allPrevousFiles.add(TransactionalDocumentSafeServiceImpl.modifyTxDocumentName(documentFQN, txID)));
            }
        }

        // Find file of the last tx
        HashSet<DocumentFQN> currentFiles = new HashSet<>();
        {
            TransactionInformation tuple = transactionInformationList.get(size - 1);
            TxIDHashMapWrapper txIDHashMapWrapper = TxIDHashMapWrapper.readHashMapOfTx(documentSafeService, userIDAuth, new LastCommitedTxID(tuple.getCurrentTxID().getValue()));
            txIDHashMapWrapper.getMap().forEach((documentFQN, txID) -> currentFiles.add(TransactionalDocumentSafeServiceImpl.modifyTxDocumentName(documentFQN, txID)));
        }
        LOGGER.debug("previous files size = " + allPrevousFiles.size());
        LOGGER.debug("current files size  = " + currentFiles.size());
        allPrevousFiles.removeAll(currentFiles);

        // delete all unused files
        LOGGER.debug("previous files size after removeing all of current file = " + allPrevousFiles.size());
        allPrevousFiles.forEach(fileToDelte -> {
            LOGGER.debug("delete " + fileToDelte);
            documentSafeService.deleteDocument(userIDAuth, fileToDelte);
        });

        // delete all metafiles of the previous tx
        {
            for (int i = 0; i < size - 1; i++) {
                TransactionInformation tuple = transactionInformationList.get(i);
                TxIDHashMapWrapper.deleteHashMapOfTx(documentSafeService, userIDAuth, new LastCommitedTxID(tuple.getCurrentTxID().getValue()));
            }
        }
        LOGGER.debug("expected HashMap to remain is " + transactionInformationList.get(size-1).getCurrentTxID());


        // clear txLog and insert only the last tx
        {
            TransactionInformation tuple = transactionInformationList.get(size - 1);
            TransactionInformationList newTransactionInformationList = new TransactionInformationList();
            newTransactionInformationList.add(tuple);
            return newTransactionInformationList;
        }
    }

}
