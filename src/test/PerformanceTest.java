package test;

import client.KVStore;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class PerformanceTest extends TestCase {

    private KVStore kvClientFIFO, kvClientLRU, kvClientLFU;
    private static int TOTAL_REQUESTS = 100;
    private static int UNIQUE_KEYS = 10;
    private static Random rand = new Random();

    @Before
    public void setUp() {
        kvClientFIFO = new KVStore("localhost", 50000);
        kvClientLRU = new KVStore("localhost", 50001);
        kvClientLFU = new KVStore("localhost", 50002);
        try {
            kvClientFIFO.connect();
            kvClientLRU.connect();
            kvClientLFU.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        kvClientFIFO.disconnect();
        kvClientLRU.disconnect();
        kvClientLFU.disconnect();
    }

    private double getTime(KVStore client, double putRatio, double getRatio) {
        String key = "spam";
        String value = "kk";
        Exception ex = null;
        long total_time = 0;

        // this does not include time for random number generation
        try {
            for (int i = 0; i < TOTAL_REQUESTS * putRatio; i++) {
                int temp = rand.nextInt(UNIQUE_KEYS);
                long startTime = System.currentTimeMillis();
                client.put(key + temp, value);
                total_time += System.currentTimeMillis() - startTime;
            }

            for (int i = 0; i < TOTAL_REQUESTS * getRatio; i++) {
                int temp = rand.nextInt(UNIQUE_KEYS);
                long startTime = System.currentTimeMillis();
                client.get(key + temp);
                total_time += System.currentTimeMillis() - startTime;
            }

        } catch (Exception e) {
            ex = e;
        }
        Assert.assertTrue(ex == null);
        return total_time/TOTAL_REQUESTS;
    }

    private void printResult(String cacheType, double time1, double time2, double time3) {
        System.out.println("<=====================================" + cacheType + "=====================================>");
        System.out.println("Average time taken per 80/20 request is: " + time1 + "ms");
        System.out.println("Average time taken per 50/50 request is: " + time2 + "ms");
        System.out.println("Average time taken per 20/80 request is: " + time3 + "ms");
    }

    @Test
    public void testFIFO() {
        double time1 = getTime(kvClientFIFO, 0.8, 0.2);
        double time2 = getTime(kvClientFIFO, 0.5, 0.5);
        double time3 = getTime(kvClientFIFO, 0.2, 0.8);
        printResult("FIFO", time1, time2, time3);
    }

    @Test
    public void testLRU() {
        double time1 = getTime(kvClientLRU, 0.8, 0.2);
        double time2 = getTime(kvClientLRU, 0.5, 0.5);
        double time3 = getTime(kvClientLRU, 0.2, 0.8);
        printResult("LRU", time1, time2, time3);
    }

    @Test
    public void testLFU() {
        double time1 = getTime(kvClientLFU, 0.8, 0.2);
        double time2 = getTime(kvClientLFU, 0.5, 0.5);
        double time3 = getTime(kvClientLFU, 0.2, 0.8);
        printResult("LFU", time1, time2, time3);
    }

}
