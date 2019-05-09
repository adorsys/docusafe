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

    public static TxIDHashMapWrapper join(TxIDHashMapWrapper stateLastCommittedTx, TxIDHashMapWrapper stateAtBeginOfCurrentTx, TxIDHashMapWrapper stateAtEndOfCurrentTx, TxIDHashMap documentsTouchedInTx) {

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
        docsTouched.addAll(documentsTouchedInTx.keySet());
        // add deleted
        MapDifference<DocumentFQN, TxID> currentTxDiff = Maps.difference(stateAtBeginOfCurrentTx.getMap(), stateAtEndOfCurrentTx.getMap());
        docsTouched.addAll(currentTxDiff.entriesOnlyOnLeft().keySet());

        MapDifference<DocumentFQN, TxID> parallelTxDiff = Maps.difference(stateLastCommittedTx.getMap(), stateAtBeginOfCurrentTx.getMap());
        List<DocumentFQN> docsTouchedInParallel = new ArrayList<>(parallelTxDiff.entriesDiffering().keySet());
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnLeft().keySet());
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnRight().keySet());

        for (DocumentFQN d : docsTouched) {
            if (docsTouchedInParallel.contains(d)) {
                TxParallelCommittingException txParallelCommittingException = new TxParallelCommittingException(stateAtBeginOfCurrentTx.getCurrentTxID(), stateLastCommittedTx.getCurrentTxID(), d.getValue());
                log.error("join begin of tx        : " + stateAtBeginOfCurrentTx.toString());
                log.error("join end of tx          : " + stateAtEndOfCurrentTx.toString());
                log.error("join last committed tx  : " + stateLastCommittedTx.toString());
                log.error("join read in current tx : " + TxIDHashMapWrapper.getString(documentsTouchedInTx));
                log.error("join result of tx       : " + txParallelCommittingException.getMessage());
                throw txParallelCommittingException;
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
            log.debug("join begin of tx        : " + stateAtBeginOfCurrentTx.toString());
            log.debug("join end of tx          : " + stateAtEndOfCurrentTx.toString());
            log.debug("join last committed tx  : " + stateLastCommittedTx.toString());
            log.debug("join read in current tx : " + TxIDHashMapWrapper.getString(documentsTouchedInTx));
            log.debug("join result of tx       : " + build.toString());
        }
        return build;

    }

}
