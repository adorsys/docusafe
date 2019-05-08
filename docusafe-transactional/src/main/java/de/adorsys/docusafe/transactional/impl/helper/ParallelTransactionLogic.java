package de.adorsys.docusafe.transactional.impl.helper;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.transactional.types.TxID;
import de.adorsys.docusafe.transactional.exceptions.TxBaseException;
import de.adorsys.docusafe.transactional.exceptions.TxParallelCommittingException;
import de.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import de.adorsys.docusafe.transactional.impl.TxIDHashMap;
import de.adorsys.docusafe.transactional.impl.TxIDHashMapWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ParallelTransactionLogic {

    public static TxIDHashMapWrapper join(TxIDHashMapWrapper stateLastCommittedTx, TxIDHashMapWrapper stateAtBeginOfCurrentTx, TxIDHashMapWrapper stateAtEndOfCurrentTx, TxIDHashMap documentsReadInTx) {

        // if no parallel commits
        TxID lastCommitedTxID = stateLastCommittedTx.getCurrentTxID();
        if (lastCommitedTxID != null && lastCommitedTxID.equals(stateAtBeginOfCurrentTx.getLastCommitedTxID())) {
            throw new TxBaseException("Nothing to merge. No parallel transactions committed.");
        }

        // changed files have same TxID as currentTxID
        Set<DocumentFQN> docsTouched = stateAtEndOfCurrentTx.getMap().entrySet().stream()
                .filter(e -> e.getValue().equals(stateAtEndOfCurrentTx.getCurrentTxID()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        // add read
        docsTouched.addAll(documentsReadInTx.keySet());
        // add deleted
        MapDifference<DocumentFQN, TxID> currentTxDiff = Maps.difference(stateAtBeginOfCurrentTx.getMap(), stateAtEndOfCurrentTx.getMap());
        docsTouched.addAll(currentTxDiff.entriesOnlyOnLeft().keySet());

        MapDifference<DocumentFQN, TxID> parallelTxDiff = Maps.difference(stateLastCommittedTx.getMap(), stateAtBeginOfCurrentTx.getMap());
        List<DocumentFQN> docsTouchedInParallel = new ArrayList<>(parallelTxDiff.entriesDiffering().keySet());
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnLeft().keySet());
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnRight().keySet());

        for(DocumentFQN d : docsTouched) {
            if(docsTouchedInParallel.contains(d)) {
                throw new TxParallelCommittingException(stateAtBeginOfCurrentTx.getCurrentTxID(), stateLastCommittedTx.getCurrentTxID(), d.getValue());
            }
        }


        TxIDHashMapWrapper build = TxIDHashMapWrapper.builder()
                .lastCommitedTxID(new LastCommitedTxID(stateLastCommittedTx.getCurrentTxID().getValue()))
                .currentTxID(new TxID())
                .beginTx(new Date())
                .endTx(new Date())
                .map(stateAtEndOfCurrentTx.getMap())
                .mergedTxID(stateAtEndOfCurrentTx.getCurrentTxID())
                .build();

        if (log.isDebugEnabled()) {
            log.debug("join input state of last committed tx: " + show(stateLastCommittedTx));
            log.debug("join input state at begin of tx      : " + show(stateAtBeginOfCurrentTx));
            log.debug("join input state of current tx       : " + show(stateAtBeginOfCurrentTx));
            log.debug("join result state of tx              : " + show(build));
        }
        return build;

    }

    private static String show(TxIDHashMapWrapper stateLastCommittedTx) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        List<DocumentFQN> list = new ArrayList<>();
        list.addAll(stateLastCommittedTx.getMap().keySet());
        Collections.sort(list, new DocumentFQNComparator());
        for (DocumentFQN documentFQN : list) {
            sb.append(documentFQN.getValue() + "." + stateLastCommittedTx.getMap().get(documentFQN).getValue());
            sb.append("\n");
        }
        return sb.toString();
    }

    public static class DocumentFQNComparator implements Comparator < DocumentFQN> {

        @Override
        public int compare(DocumentFQN o1, DocumentFQN o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    }
}
