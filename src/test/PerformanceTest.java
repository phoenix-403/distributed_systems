package test;

import app_kvClient.KVClient;
import app_kvECS.EcsException;
import client.KVStore;
import ecs.ECSNode;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static org.junit.Assert.assertTrue;

public class PerformanceTest  {

    @Test
    public void kk() throws Exception {
        testRead("ecsTest1.config", 10);
        assertTrue(true);
    }

    private KVStore connectAny(String configFile) {
        File file = new File("src/app_kvECS/" + configFile);

        final String DELIMITER = " ";
        final String DELIMITER_PATTERN = Pattern.quote(DELIMITER);

        ArrayList<String> fileLines = null;
        try {
            fileLines = (ArrayList<String>) Files.readAllLines(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String line : fileLines) {
            String[] tokenizedLine = line.split(DELIMITER_PATTERN);
            try {
                KVStore store = new KVStore(null, tokenizedLine[1], Integer.parseInt(tokenizedLine[2]));
                store.connect();
                store.disconnect();
                return (store);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public void testRead(String configFile, int limit) throws Exception {

        final DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("/home/k/maildir"));
        ArrayList<KVClient> kvClients = new ArrayList<>();

        KVStore defaultKV = connectAny(configFile);
        long time = 0;
        int i = 0;

        for (Path dir : dirStream) {
            HashMap<String, String> keyValues = new HashMap<>();
            Stream<Path> files = Files.walk(dir);
            Iterator it = files.iterator();
            KVClient temp = new KVClient();
            new Thread(temp).start();
            temp.newConnection(defaultKV.getAddress(), defaultKV.getPort());

            while (it.hasNext()) {
                String p = it.next().toString();
                if (!new File(p).isDirectory())
                    keyValues.put(p, new String(Files.readAllBytes(Paths.get(p))) );
            }
            temp.performanceTest(keyValues);
            kvClients.add(temp);
            if (i++ >= limit)
                break;
        }
    }



}
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
