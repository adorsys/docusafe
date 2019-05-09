package de.adorsys.docusafe.transactional.impl;

import de.adorsys.docusafe.transactional.exceptions.NoTxFoundForDocumentException;
import de.adorsys.docusafe.transactional.impl.helper.BucketContentFromHashMapHelper;
import de.adorsys.docusafe.transactional.impl.helper.Class2JsonHelper;
import de.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import de.adorsys.docusafe.transactional.types.TxID;
import lombok.*;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.exceptions.TxAlreadyClosedException;
import de.adorsys.docusafe.transactional.exceptions.TxNotFoundException;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 11.06.18 at 15:12.
 */

/**
 * Contains all documents, that exist for this transaction. the documents have been created from previous or this transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TxIDHashMapWrapper {
    private final static Logger LOGGER = LoggerFactory.getLogger(TxIDHashMapWrapper.class);
    private final static DocumentFQN filenamebase = new DocumentFQN("TransactionalHashMap.txt");

    private TxID mergedTxID = null;
    private LastCommitedTxID lastCommitedTxID;
    private TxID currentTxID;
    private Date beginTx;
    private Date endTx;
    private TxIDHashMap map = new TxIDHashMap();

    private TxIDHashMapWrapper(LastCommitedTxID lastCommitedTxID, TxID currentTx, Date beginTxDate) {
        this.lastCommitedTxID = lastCommitedTxID;
        this.currentTxID = currentTx;
        this.beginTx = beginTxDate;
        if (this.lastCommitedTxID == null) {
            this.lastCommitedTxID = new LastCommitedTxID("NULL");
        }
    }

    public TxIDHashMapWrapper clone() {
        TxIDHashMapWrapper newTxIDHashMapWrapper = new TxIDHashMapWrapper(lastCommitedTxID, currentTxID, beginTx);
        newTxIDHashMapWrapper.map.putAll(map);
        return newTxIDHashMapWrapper;
    }

    public static TxIDHashMapWrapper fromPreviousFileOrNew(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, TxID currentTxID, Date beginTxDate) {
        LastCommitedTxID lastKnownCommitedTxID = TxIDLog.findLastCommitedTxID(documentSafeService, userIDAuth);

        if (lastKnownCommitedTxID == null) {
            return new TxIDHashMapWrapper(lastKnownCommitedTxID, currentTxID, beginTxDate);
        }

        DocumentFQN file = TransactionalDocumentSafeServiceImpl.modifyTxMetaDocumentName(filenamebase, lastKnownCommitedTxID);
        TxIDHashMapWrapper map = readHashMapOfTx(documentSafeService, userIDAuth, lastKnownCommitedTxID);
        map.lastCommitedTxID = new LastCommitedTxID(map.currentTxID.getValue());
        map.currentTxID = currentTxID;
        map.beginTx = beginTxDate;
        map.endTx = null;
        return map;
    }

    public static TxIDHashMapWrapper readHashMapOfTx(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, LastCommitedTxID lastCommitedTxID) {
        DocumentFQN file = TransactionalDocumentSafeServiceImpl.modifyTxMetaDocumentName(filenamebase, lastCommitedTxID);
        if (!documentSafeService.documentExists(userIDAuth, file)) {
            throw new TxNotFoundException(file, lastCommitedTxID);
        }
        DSDocument dsDocument = documentSafeService.readDocument(userIDAuth, file);
        return new Class2JsonHelper().txidHashMapFromContent(dsDocument.getDocumentContent());
    }

    public static void deleteHashMapOfTx(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, LastCommitedTxID lastCommitedTxID) {
        DocumentFQN file = TransactionalDocumentSafeServiceImpl.modifyTxMetaDocumentName(filenamebase, lastCommitedTxID);
        if (!documentSafeService.documentExists(userIDAuth, file)) {
            throw new TxNotFoundException(file, lastCommitedTxID);
        }
        LOGGER.debug("delete transactional HashMap file" + file);
        documentSafeService.deleteDocument(userIDAuth, file);
    }


    public void saveOnce(DocumentSafeService documentSafeService, UserIDAuth userIDAuth) {
        DocumentFQN file = TransactionalDocumentSafeServiceImpl.modifyTxMetaDocumentName(filenamebase, currentTxID);
        LOGGER.debug("save " + file.getValue());
        DocumentContent documentContent = new Class2JsonHelper().txidHashMapToContent(this);
        DSDocument dsDocument = new DSDocument(file, documentContent);
        documentSafeService.storeDocument(userIDAuth, dsDocument);
    }

    public void storeDocument(DocumentFQN documentFQN) {
        map.put(documentFQN, currentTxID);
    }

    public TxID getTxIDOfDocument(DocumentFQN documentFQN) {
        TxID txID = map.get(documentFQN);
        if (txID == null) {
            throw new NoTxFoundForDocumentException(documentFQN);
        }
        return txID;
    }

    public void deleteDocument(DocumentFQN documentFQN) {
        map.remove(documentFQN);
    }

    public boolean documentExists(DocumentFQN documentFQN) {
        return map.containsKey(documentFQN);
    }

    public void deleteFolder(DocumentDirectoryFQN documentDirectoryFQN) {
        // Es müssen einfach alle Einträge gelöscht werden, die zu diesem directory gehören
        List<DocumentFQN> entriesToRemove = new ArrayList<>();
        map.keySet().forEach(documentFQN -> {
            if (documentFQN.getValue().startsWith(documentDirectoryFQN.getValue())) {
                entriesToRemove.add(documentFQN);
            }
        });
        entriesToRemove.forEach(documentFQN -> deleteDocument(documentFQN));
    }

    public void setEndTransactionDate(Date endTransactionDate) {
        endTx = endTransactionDate;
    }

    public void transactionIsOver(DocumentSafeService documentSafeService, UserIDAuth userIDAuth, CurrentTransactionData currentTransactionData) {
        TxIDLog.saveJustFinishedTx(documentSafeService, userIDAuth, currentTransactionData);
    }

    public TxBucketContentFQN list(DocumentDirectoryFQN documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
        return BucketContentFromHashMapHelper.list(map, documentDirectoryFQN, recursiveFlag);
    }

    public void checkTxStillOpen() {
        if (endTx != null) {
            throw new TxAlreadyClosedException(currentTxID);
        }
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("TxIDHashMapWrapper {");
        sb.append("\n");
        sb.append("  merged tx        : " + mergedTxID);
        sb.append("\n");
        sb.append("  last commited tx : " + lastCommitedTxID);
        sb.append("\n");
        sb.append("  current tx       : " + currentTxID);
        sb.append("\n");
        sb.append("  begin - end      : " + getString(beginTx) + " - " + getString(endTx));
        sb.append("\n");
        sb.append(getString(map));
        sb.append("}\n");

        return sb.toString();
    }

    static class DocumentFQNComparator implements Comparator< DocumentFQN> {
        @Override
        public int compare(DocumentFQN o1, DocumentFQN o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    }

    private String getString(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss,SSS");
        return sdf.format(date);
    }

    public static String getString(TxIDHashMap map) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        List<DocumentFQN> list = new ArrayList<>();
        list.addAll(map.keySet());
        Collections.sort(list, new DocumentFQNComparator());
        for (DocumentFQN documentFQN : list) {
            sb.append("  " + documentFQN.getValue() + "." + map.get(documentFQN).getValue());
            sb.append("\n");
        }
        return sb.toString();
    }
}
