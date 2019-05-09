package de.adorsys.docusafe.transactional;

import com.googlecode.catchexception.CatchException;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.transactional.exceptions.TxParallelCommittingException;
import de.adorsys.docusafe.transactional.impl.TxIDHashMap;
import de.adorsys.docusafe.transactional.impl.TxIDHashMapWrapper;
import de.adorsys.docusafe.transactional.impl.helper.ParallelTransactionLogic;
import de.adorsys.docusafe.transactional.types.TxID;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class ParallelTransactionLogicTest {

    private static final TxID OLD_TX_ID = new TxID("OLD_TX");
    private static final TxID CURRENT_TX_ID = new TxID("CURRENT_TX");
    private static final TxID PARALLEL_TX_ID_1 = new TxID("PARALLEL_TX_1");
    private static final TxID PARALLEL_TX_ID_2 = new TxID("PARALLEL_TX_2");
    private static final TxID LAST_COMMIT_TX = new TxID("LAST_COMMIT_TX");

    @Test
    public void parallelCreationSameFile() {
        log.info("parallelCreationSameFile");
        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(OLD_TX_ID)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), LAST_COMMIT_TX);
        lastCommitMap.put(new DocumentFQN("B"), PARALLEL_TX_ID_1);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(LAST_COMMIT_TX)
                .lastCommitedTxID(OLD_TX_ID)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(OLD_TX_ID)
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();
        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx));
        checkConflictFile(new DocumentFQN("A"));
    }

    @Test
    public void parallelCreationDifferentFiles() {
        log.info("parallelCreationDifferentFiles");
        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(OLD_TX_ID)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), LAST_COMMIT_TX);
        lastCommitMap.put(new DocumentFQN("B"), PARALLEL_TX_ID_1);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(LAST_COMMIT_TX)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("C"), CURRENT_TX_ID);
        endStateMap.put(new DocumentFQN("D"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(LAST_COMMIT_TX)
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();

        TxIDHashMapWrapper result = ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx,
                stateAtEndOfCurrentTx, documentsReadInTx);

        TxIDHashMap expected = new TxIDHashMap();
        expected.putAll(lastCommitMap);
        expected.putAll(endStateMap);
        Assert.assertEquals(expected, result.getMap());
        // Assert.assertTrue(result.getMergedTxID().equals(LAST_COMMIT_TX));
        // Assert.assertTrue(result.getLastCommitedTxID().equals(CURRENT_TX_ID));
    }

    @Test
    public void readFileUpdatedInParallelTx() {
        log.info("readFileUpdatedInParallelTx");
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(OLD_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), LAST_COMMIT_TX);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(LAST_COMMIT_TX)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("C"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(OLD_TX_ID)
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();
        documentsReadInTx.put(new DocumentFQN("A"), OLD_TX_ID);

        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx));
        checkConflictFile(new DocumentFQN("A"));
    }

    @Test
    public void readFileDeletedInParallelTx() {
        log.info("readFileDeletedInParallelTx");
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(OLD_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), LAST_COMMIT_TX);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(LAST_COMMIT_TX)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("C"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(OLD_TX_ID)
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();
        documentsReadInTx.put(new DocumentFQN("B"), OLD_TX_ID);

        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx));
        checkConflictFile(new DocumentFQN("B"));

    }

    @Test
    public void updateFileCDeletedInParallelTx() {
        log.info("updateFileCreatedAndDeletedInParallelTx");
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(OLD_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("B"), LAST_COMMIT_TX);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(LAST_COMMIT_TX)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), CURRENT_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(OLD_TX_ID)
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();

        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx,
                stateAtEndOfCurrentTx, documentsReadInTx));
        checkConflictFile(new DocumentFQN("A"));
    }

    @Test
    public void createFileCreatedInParallelTx() {
        log.info("createFileCreatedInParallelTx");
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(OLD_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), OLD_TX_ID);
        lastCommitMap.put(new DocumentFQN("B"), LAST_COMMIT_TX);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(LAST_COMMIT_TX)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("B"), CURRENT_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(OLD_TX_ID)
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();

        CatchException.catchException(() -> ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx,
                stateAtEndOfCurrentTx, documentsReadInTx));
        checkConflictFile(new DocumentFQN("B"));
    }


    @Test
    public void readFileUnchancedInParallelTx() {
        log.info("readFileUnchancedInParallelTx");
        TxIDHashMap beginStateMap = new TxIDHashMap();
        beginStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        beginStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtBeginOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(OLD_TX_ID)
                .map(beginStateMap)
                .build();

        TxIDHashMap lastCommitMap = new TxIDHashMap();
        lastCommitMap.put(new DocumentFQN("A"), OLD_TX_ID);
        lastCommitMap.put(new DocumentFQN("B"), OLD_TX_ID);
        lastCommitMap.put(new DocumentFQN("C"), PARALLEL_TX_ID_1);
        lastCommitMap.put(new DocumentFQN("D"), PARALLEL_TX_ID_2);

        TxIDHashMapWrapper stateLastCommittedTx = new TxIDHashMapWrapper().toBuilder()
                .currentTxID(LAST_COMMIT_TX)
                .map(lastCommitMap)
                .build();


        TxIDHashMap endStateMap = new TxIDHashMap();
        endStateMap.put(new DocumentFQN("A"), OLD_TX_ID);
        endStateMap.put(new DocumentFQN("B"), OLD_TX_ID);

        TxIDHashMapWrapper stateAtEndOfCurrentTx = new TxIDHashMapWrapper().toBuilder()
                .lastCommitedTxID(OLD_TX_ID)
                .currentTxID(CURRENT_TX_ID)
                .map(endStateMap)
                .build();

        TxIDHashMap documentsReadInTx = new TxIDHashMap();
        documentsReadInTx.put(new DocumentFQN("A"), OLD_TX_ID);

        TxIDHashMapWrapper result = ParallelTransactionLogic.join(stateLastCommittedTx, stateAtBeginOfCurrentTx, stateAtEndOfCurrentTx, documentsReadInTx);

        TxIDHashMap expected = lastCommitMap;
        Assert.assertEquals(expected, result.getMap());
    }

    private void checkConflictFile(DocumentFQN a) {
        Exception e = CatchException.caughtException();
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof TxParallelCommittingException);
        TxParallelCommittingException p = (TxParallelCommittingException) e;
        Assert.assertEquals(a, ((TxParallelCommittingException) e).getConflictDocument());
    }


}
