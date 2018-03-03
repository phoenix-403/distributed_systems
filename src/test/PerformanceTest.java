//package test;
//
//import app_kvServer.KVServer;
//import client.KVStore;
//import junit.framework.TestCase;
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.util.Random;
//
//public class PerformanceTest extends TestCase {
//
//    private KVStore kvClientFIFO, kvClientLRU, kvClientLFU;
//    private static int TOTAL_REQUESTS = 400;
//    private static int UNIQUE_KEYS = 100;
//    private static Random rand = new Random();
//
//    private double getTime(KVStore client, double putRatio, double getRatio) {
//        String key = "spam";
//        String value = "kk";
//        Exception ex = null;
//        long total_time = 0;
//
//        // this does not include time for random number generation
//        try {
//            for (int i = 0; i < TOTAL_REQUESTS * putRatio; i++) {
//                int temp = rand.nextInt(UNIQUE_KEYS);
//                long startTime = System.currentTimeMillis();
//                client.put(key + temp, value);
//                total_time += System.currentTimeMillis() - startTime;
//            }
//
//            for (int i = 0; i < TOTAL_REQUESTS * getRatio; i++) {
//                int temp = rand.nextInt(UNIQUE_KEYS);
//                long startTime = System.currentTimeMillis();
//                client.get(key + temp);
//                total_time += System.currentTimeMillis() - startTime;
//            }
//
//        } catch (Exception e) {
//            ex = e;
//        }
//        Assert.assertTrue(ex == null);
//        return total_time/TOTAL_REQUESTS;
//    }
//
//    private void printResult(String[] stuff) {
//        System.out.println("<=====================" + stuff[0] + "=====================>");
//        System.out.println("Average time taken per 80/20 request is: " + stuff[1] + "ms");
//        System.out.println("Average time taken per 50/50 request is: " + stuff[2] + "ms");
//        System.out.println("Average time taken per 20/80 request is: " + stuff[3] + "ms");
//    }
//
//    public String[] testFIFO() {
//        double time1 = getTime(kvClientFIFO, 0.8, 0.2);
//        double time2 = getTime(kvClientFIFO, 0.5, 0.5);
//        double time3 = getTime(kvClientFIFO, 0.2, 0.8);
//        return new String[]{"FIFO", Double.toString(time1), Double.toString(time2), Double.toString(time3)};
//    }
//
//    public String[] testLRU() {
//        double time1 = getTime(kvClientLRU, 0.8, 0.2);
//        double time2 = getTime(kvClientLRU, 0.5, 0.5);
//        double time3 = getTime(kvClientLRU, 0.2, 0.8);
//        return new String[]{"LRU", Double.toString(time1), Double.toString(time2), Double.toString(time3)};
//    }
//
//    public String[] testLFU() {
//        double time1 = getTime(kvClientLFU, 0.8, 0.2);
//        double time2 = getTime(kvClientLFU, 0.5, 0.5);
//        double time3 = getTime(kvClientLFU, 0.2, 0.8);
//        return new String[]{"LFU", Double.toString(time1), Double.toString(time2), Double.toString(time3)};
//    }
//
//    @Test
//    public void testCacheSize10() {
//        setUp(10);
//        String[] batch1 = testFIFO();
//        String[] batch2 = testLRU();
//        String[] batch3 = testLFU();
//        System.out.println("<xxxxxxxxxxxxxxxxxxxxCache Size 10xxxxxxxxxxxxxxxxxxxx>\n\n");
//        printResult(batch1);
//        printResult(batch2);
//        printResult(batch3);
//        tearDown();
//    }
//
//    @Test
//    public void testCacheSize50() {
//        setUp(50);
//        String[] batch1 = testFIFO();
//        String[] batch2 = testLRU();
//        String[] batch3 = testLFU();
//        System.out.println("<xxxxxxxxxxxxxxxxxxxxCache Size 50xxxxxxxxxxxxxxxxxxxx>\n\n");
//        printResult(batch1);
//        printResult(batch2);
//        printResult(batch3);
//        tearDown();
//    }
//
//    @Test
//    public void testCacheSize100() {
//        setUp(100);
//        String[] batch1 = testFIFO();
//        String[] batch2 = testLRU();
//        String[] batch3 = testLFU();
//        System.out.println("<xxxxxxxxxxxxxxxxxxxxCache Size 100xxxxxxxxxxxxxxxxxxxx>\n\n");
//        printResult(batch1);
//        printResult(batch2);
//        printResult(batch3);
//        tearDown();
//    }
//
//    // todo fix
//    public void setUp(int cacheSize) {
//        new Thread(new KVServer(50003, cacheSize, "FIFO")).start();
//        new Thread(new KVServer(50004, cacheSize, "LRU")).start();
//        new Thread(new KVServer(50005, cacheSize, "LFU")).start();
//        kvClientFIFO = new KVStore("localhost", 50003);
//        kvClientLRU = new KVStore("localhost", 50004);
//        kvClientLFU = new KVStore("localhost", 50005);
//        try {
//            kvClientFIFO.connect();
//            kvClientLRU.connect();
//            kvClientLFU.connect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void tearDown() {
//        kvClientFIFO.disconnect();
//        kvClientLRU.disconnect();
//        kvClientLFU.disconnect();
//    }
//
//
//
//}
