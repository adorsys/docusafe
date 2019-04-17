package org.adorsys.docusafe.transactional.impl;

import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.transactional.types.TxID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by peter on 11.07.18 at 10:33.
 */

/**
 * Each user has its own context and thus its own transactional space.
 * This map contains the current txmap of the user.
 */
public class CurrentTransactionData {
    private final static Logger LOGGER = LoggerFactory.getLogger(CurrentTransactionData.class);
    private TxID currentTxID = null;
    private TxIDHashMapWrapper currentTxHashMap = null;
    private TxIDHashMapWrapper initialTxHashMap = null;
    private TxIDHashMap documentsReadInThisTx = new TxIDHashMap();
    private Set<DocumentFQN> nonTxInboxDocumentsToBeDeletedAfterCommit = new HashSet<>();

    public CurrentTransactionData(TxID currentTxID, TxIDHashMapWrapper currentTxHashMap) {
        this.currentTxID = currentTxID;
        this.currentTxHashMap = currentTxHashMap;
        initialTxHashMap = currentTxHashMap.clone();
    }

    public TxID getCurrentTxID() {
        return currentTxID;
    }

    public TxIDHashMapWrapper getCurrentTxHashMap() {
        return currentTxHashMap;
    }

    public TxIDHashMapWrapper getInitialTxHashMap() {
        return initialTxHashMap;
    }

    public TxIDHashMap getDocumentsReadInThisTx() {
        return documentsReadInThisTx;
    }

    

    public boolean anyDifferenceToInitalState() {
        Set<DocumentFQN> currentFQNs = new HashSet<>(currentTxHashMap.getMap().keySet());
        Set<DocumentFQN> initialFQNs = new HashSet<>(initialTxHashMap.getMap().keySet());
        if (currentFQNs.size() > initialFQNs.size()) {
            currentFQNs.removeAll(initialFQNs);
            currentFQNs.forEach(fqn -> LOGGER.debug(" new file has been created: " + fqn));
            return true;
        }
        if (currentFQNs.size() < initialFQNs.size()) {
            initialFQNs.removeAll(currentFQNs);
            initialFQNs.forEach(fqn -> LOGGER.debug(" old file has been removed: " + fqn));
            return true;
        }
        for (DocumentFQN fqn : currentFQNs) {
            if (!initialFQNs.contains(fqn)) {
                LOGGER.debug(" old file has been removed: " + fqn);
                return true;
            }
            TxID currentTxID = currentTxHashMap.getMap().get(fqn);
            TxID initialTxID = initialTxHashMap.getMap().get(fqn);
            if (!currentTxID.equals(initialTxID)) {
                LOGGER.debug(" old file has changed:" + fqn);
                return true;
            }
        }
        LOGGER.debug(" nothing has changed");
        return false;
    }

    public void addNonTxInboxFileToBeDeletedAfterCommit(DocumentFQN nonTxFQN) {
        nonTxInboxDocumentsToBeDeletedAfterCommit.add(nonTxFQN);
    }

    public Set<DocumentFQN> getNonTxInboxDocumentsToBeDeletedAfterCommit() {
        return nonTxInboxDocumentsToBeDeletedAfterCommit;
    }
}
