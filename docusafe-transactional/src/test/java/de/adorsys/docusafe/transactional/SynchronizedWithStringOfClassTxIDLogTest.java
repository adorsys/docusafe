package de.adorsys.docusafe.transactional;

import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.common.utils.HexUtil;
import de.adorsys.docusafe.service.api.types.UserID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Created by peter on 03.12.18 14:33.
 * This method proves, that synchronizing the method TxIDLog.saveJustFinishedTx with the userid is ok.
 * The method will become blocked for the same user, but not for different users.
 */
public class SynchronizedWithStringOfClassTxIDLogTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(SynchronizedWithStringOfClassTxIDLogTest.class);

    @Test
    public void testMethodsaveJustFinishedTxForSameString() {
        testMethodsaveJustFinishedTx(100, false);
    }

    @Test
    public void testMethodsaveJustFinishedTxForDifferentString() {
        testMethodsaveJustFinishedTx(100, true);
    }

    // @Test
    // ist 42 mal gut gegangen, ist wohl ok
    public void lasttest() {
        int counter = 0;
        do {
            LOGGER.info("COUNTER IS NOW " + counter++);
            testMethodsaveJustFinishedTx(100, false);
            testMethodsaveJustFinishedTx(100, true);
        }
        while (true);


    }

    public void testMethodsaveJustFinishedTx(int WAIT, boolean differ) {
        try {
            int MAX_DELTA = 20; // maximal 10 Millisekunden dürfen für die Zeitmessung vergehen

            Semaphore semaphore = new Semaphore(2);
            CountDownLatch countDownLatch = new CountDownLatch(2);
            semaphore.acquire(2);

            SynchronizedWithStringOfClassTxIDLogTest.ARunnable runnable1 = new SynchronizedWithStringOfClassTxIDLogTest.ARunnable(semaphore, countDownLatch, WAIT, false);
            SynchronizedWithStringOfClassTxIDLogTest.ARunnable runnable2 = new SynchronizedWithStringOfClassTxIDLogTest.ARunnable(semaphore, countDownLatch, WAIT, differ);
            Thread[] instances = new Thread[2];
            instances[0] = new Thread(runnable1);
            instances[1] = new Thread(runnable2);
            instances[0].start();
            instances[1].start();

            LOGGER.info("prepare for start");
            Thread.currentThread().sleep(2000);
            LOGGER.info("GO");

            semaphore.release(2);
            LOGGER.info("wait for two instances to finsih");
            countDownLatch.await();

            long fast = Math.min(runnable1.durationInMillis, runnable2.durationInMillis);
            long slow = Math.max(runnable1.durationInMillis, runnable2.durationInMillis);
            LOGGER.info("fast thread took " + fast);
            LOGGER.info("slow thread took " + slow);

            if (differ) {
                // Wenn auf unterschiedlichen Namen gearbeitet wurde, dann dürfen beide maxiaml die Wartezeit + Delta gebraucht haben
                LOGGER.info("assume both threads did not run longer than " + (WAIT + MAX_DELTA));
                Assert.assertTrue(fast < (WAIT + MAX_DELTA));
                Assert.assertTrue(slow < (WAIT + MAX_DELTA));
            } else {
                // Berechne die zeit, die der langsamere Thread später gestartet ist:
                long starttimeOfFastThread = fast == runnable1.durationInMillis ? runnable1.starttime : runnable2.starttime;
                long starttimeOfSlowThread = fast == runnable1.durationInMillis ? runnable2.starttime : runnable1.starttime;
                long delay = starttimeOfSlowThread - starttimeOfFastThread;

                LOGGER.info("starttime of fast thread was " + getString(new Date(starttimeOfFastThread)));
                LOGGER.info("starttime of slow thread was " + getString(new Date(starttimeOfSlowThread)));
                LOGGER.info("delay is " + delay);

                LOGGER.info("assume fast threads did not run longer than " + (WAIT + MAX_DELTA));
                Assert.assertTrue(fast < (WAIT + MAX_DELTA));
                LOGGER.info("assume slow threads did not run longer than " + (WAIT + MAX_DELTA + (WAIT - delay)));
                Assert.assertTrue(slow < (WAIT + MAX_DELTA + (WAIT - delay)));
                LOGGER.info("assume slow threads did run longer or equal than " + (WAIT + (WAIT - delay)));
                Assert.assertTrue(slow >= (WAIT + (WAIT - delay)));
            }
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }


    }

    public static class ARunnable implements Runnable {
        private int timeToBlock;
        private UserID key;
        private Semaphore semaphore;
        private CountDownLatch countDownLatch;
        public long durationInMillis = -1;
        public long starttime = -1;
        public long endtime = -1;
        private Blocker blocker = new Blocker();

        public ARunnable(Semaphore sem, CountDownLatch countDownLatch, int timeToBlock, boolean differ) {
            this.timeToBlock = timeToBlock;
            this.semaphore = sem;
            this.countDownLatch = countDownLatch;

            // looks weired, but just to make sure a new String is used.
            this.key = new UserID(new String(HexUtil.convertHexStringToBytes(HexUtil.convertBytesToHexString((differ ? "bifi" : "affe").getBytes()))));
        }

        @Override
        public void run() {
            try {
                semaphore.acquire();
                LOGGER.info("start for key " + key);
                starttime = new Date().getTime();
                blocker.blockForString(key, timeToBlock);
                endtime = new Date().getTime();
                this.durationInMillis = endtime - starttime;
                LOGGER.info("finsih for key " + key);
                semaphore.release();
            } catch (Exception e) {
                throw BaseExceptionHandler.handle(e);
            } finally {
                countDownLatch.countDown();
            }

        }

    }

    private String getString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss,SSS");
        return sdf.format(date);
    }

    private static class Blocker {
        private final static Logger LOGGER = LoggerFactory.getLogger(SynchronizedWithStringOfClassTxIDLogTest.class);
        private void blockForString(UserID s, int timeToBlock) {
            // THIS IS VERY IMPORTANT String.intern() see http://antkorwin.com/concurrency/synchronization_by_value.html<12
            synchronized (s.getValue().intern()) {
                LOGGER.info("start method for " + s);
                try {
                    Thread.currentThread().sleep(timeToBlock);
                } catch (Exception e) {
                    throw BaseExceptionHandler.handle(e);
                }
                LOGGER.info("finish method for " + s);
            }
        }

    }
}
