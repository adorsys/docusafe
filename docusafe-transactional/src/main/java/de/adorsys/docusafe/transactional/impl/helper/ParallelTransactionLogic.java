package de.adorsys.docusafe.transactional.impl.helper;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.transactional.types.TxID;
import de.adorsys.docusafe.transactional.exceptions.TxBaseException;
import de.adorsys.docusafe.transactional.exceptions.TxParallelCommittingException;
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
        // so docsTouched contains all files that have been created or updated in the current tx
        Set<DocumentFQN> docsTouched = stateAtEndOfCurrentTx.getMap().entrySet().stream()
                .filter(e -> e.getValue().equals(stateAtEndOfCurrentTx.getCurrentTxID()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());


        // add read
        // now docsTouched contains all files that have been created or updated or read in the current tx
        docsTouched.addAll(documentsTouchedInTx.keySet());

        // add deleted
        // now docsTouched contains all files that have been created or updated or read or deleted in the current tx
        MapDifference<DocumentFQN, TxID> currentTxDiff = Maps.difference(stateAtBeginOfCurrentTx.getMap(), stateAtEndOfCurrentTx.getMap());
        docsTouched.addAll(currentTxDiff.entriesOnlyOnLeft().keySet());

        // parallelTxDiff is created with all files that have been created or updated in parallel to the current tx
        MapDifference<DocumentFQN, TxID> parallelTxDiff = Maps.difference(stateLastCommittedTx.getMap(), stateAtBeginOfCurrentTx.getMap());

        // docsTouchedInParallel contains the filenames only that existed in the last committed tx
        List<DocumentFQN> docsTouchedInParallel = new ArrayList<>(parallelTxDiff.entriesDiffering().keySet());

        // now all files of the last committed tx are added
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnLeft().keySet());

        // now all files of the state at the beginning of the current tx are added
        docsTouchedInParallel.addAll(parallelTxDiff.entriesOnlyOnRight().keySet());

        // if there is one file that has been created, deleted, updated in parallel, an exception is raised
        for (DocumentFQN d : docsTouched) {
            if (docsTouchedInParallel.contains(d)) {
                TxParallelCommittingException txParallelCommittingException = new TxParallelCommittingException(stateAtEndOfCurrentTx.getCurrentTxID(), stateLastCommittedTx.getCurrentTxID(), d);
                log.error("join begin of tx        : " + stateAtBeginOfCurrentTx.toString());
                log.error("join end of tx          : " + stateAtEndOfCurrentTx.toString());
                log.error("join last committed tx  : " + stateLastCommittedTx.toString());
                log.error("join read in current tx : " + TxIDHashMapWrapper.getString(documentsTouchedInTx));
                log.error("join result of tx       : " + txParallelCommittingException.getMessage());
                throw txParallelCommittingException;
            }
        }

        // now join the the result
        TxIDHashMap joinedMap = new TxIDHashMap();
        joinedMap.putAll(stateAtEndOfCurrentTx.getMap());
        joinedMap.putAll(stateLastCommittedTx.getMap());

        TxIDHashMapWrapper build = TxIDHashMapWrapper.builder()
                .lastCommitedTxID(stateAtEndOfCurrentTx.getCurrentTxID())
                .currentTxID(new TxID())
                .beginTx(new Date())
                .endTx(new Date())
                .map(joinedMap)
                .mergedTxID(stateLastCommittedTx.getCurrentTxID())
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
