package de.adorsys.docusafe.transactional;

import de.adorsys.common.exceptions.BaseExceptionHandler;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Created by peter on 13.06.18 at 08:39.
 */
public class ParallelUUIDTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(ParallelUUIDTest.class);
    private final static int PARALLEL_INSTANCES = 100;

    @Test
    public void createUUIDs() {
        try {
            

            Semaphore semaphore = new Semaphore(PARALLEL_INSTANCES);
            CountDownLatch countDownLatch = new CountDownLatch(PARALLEL_INSTANCES);
            semaphore.acquire(PARALLEL_INSTANCES);

            ARunnable[] runnables = new ARunnable[PARALLEL_INSTANCES];
            Thread[] instances = new Thread[PARALLEL_INSTANCES];
            for (int i = 0; i < PARALLEL_INSTANCES; i++) {
                runnables[i] = new ARunnable(countDownLatch, semaphore);
                instances[i] = new Thread(runnables[i]);
                instances[i].start();
            }
            Thread.currentThread().sleep(2000);
            UUID uuidFirst = com.datastax.driver.core.utils.UUIDs.timeBased();
            LOGGER.debug("start " + PARALLEL_INSTANCES + " threads concurrently now");
            semaphore.release(PARALLEL_INSTANCES);

            countDownLatch.await();
            UUID uuidLast = com.datastax.driver.core.utils.UUIDs.timeBased();
            Set<UUID> uuids = new HashSet<>();
            for (int i = 0; i < PARALLEL_INSTANCES; i++) {
                uuids.add(runnables[i].uuid);
            }
            Assert.assertEquals(PARALLEL_INSTANCES, uuids.size());
            // Just to make sure duplicates are ignored
            uuids.add(uuids.stream().findFirst().get());
            Assert.assertEquals(PARALLEL_INSTANCES, uuids.size());

            LOGGER.debug("finished " + PARALLEL_INSTANCES + " threads in parallel created " + PARALLEL_INSTANCES + " different uuids");
            List<UUID> uuidList = new ArrayList<>();
            uuidList.add(uuidFirst);
            uuidList.add(uuidLast);
            uuidList.addAll(uuids);
            Collections.sort(uuidList);
            Assert.assertEquals(uuidFirst, uuidList.get(0));
            Assert.assertEquals(uuidLast, uuidList.get(PARALLEL_INSTANCES+1));

            for (int i = 0; i < uuidList.size(); i++) {
                LOGGER.debug("[" + String.format("%3d",i) + "] " + uuidList.get(i).toString());
            }
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public static class ARunnable implements Runnable {
        private final static Logger LOGGER = LoggerFactory.getLogger(ARunnable.class);
        private static int instanceCounter = 0;

        private int instanceID;
        private Semaphore sem;
        private CountDownLatch countDownLatch;
        public UUID uuid;

        public ARunnable(CountDownLatch countDownLatch, Semaphore semaphore) {
            instanceID = instanceCounter++;
            sem = semaphore;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                sem.acquire();
                // uuid = UUID.randomUUID();
                uuid = com.datastax.driver.core.utils.UUIDs.timeBased();
                sem.release();
            } catch (Exception e) {
                LOGGER.error("Exception " + e);
                throw BaseExceptionHandler.handle(e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}
