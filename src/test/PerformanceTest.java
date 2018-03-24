package test;

import app_kvClient.KVClient;
import app_kvECS.ECSClient;
import app_kvECS.EcsException;
import client.KVStore;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PerformanceTest  {
    ECSClient ecsClient;
    String configFile;

    int[] clientNumbers = new int[]{1, 5, 10, 50, 100};
    int clientCountIndex=-1;


    long averageTime=0;
    long averageTimeSum =0;

    long sampleCount =1;
    long totalClients;
    long totalServers;

    private static Logger logger = LogManager.getLogger(PerformanceTest.class);

    public static void main(String[] args) throws Exception {
//        PerformanceTest test = new PerformanceTest(args[0]);
//        test.testEmailData(Integer.parseInt(args[1]));


//        PerformanceTest test = new PerformanceTest(args[0], 0);
        PerformanceTest test = new PerformanceTest(args[0], "FIFO");

    }

    public synchronized void updateAverage(long time){
        averageTimeSum +=time;
        averageTime= averageTimeSum / sampleCount;
        sampleCount++;
        if (sampleCount >= totalClients) {
            try {
                logger.debug("Overall Average RTT time for " + totalClients + " clients " +
                        "and " + totalServers + " servers is " + averageTime);
                logger.debug("Overall Average Throughput time for " + totalClients + " clients " +
                        "and " + totalServers + " servers is " + 100/averageTime);
                ecsClient.shutdown();
                ecsClient.stopZK();
                if (clientCountIndex != -1)
                    new PerformanceTest(configFile, clientCountIndex + 1);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    PerformanceTest(String configFile, String strategy) throws Exception {
        this.configFile = configFile;
        ecsClient = new ECSClient("localhost", 2181);
        ecsClient.startZK();
        totalServers = Files.lines(Paths.get(new File("src/app_kvECS/" + configFile).getPath())).count();
        ecsClient.addNodes((int) totalServers, strategy, 20);
        ecsClient.start();
        this.testEmailData(1);
        try {
            new LogSetup("logs/test/Performance.log", Level.DEBUG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    PerformanceTest(String configFile, int index) throws Exception {
        this.clientCountIndex = index;
        this.configFile = configFile;
        ecsClient = new ECSClient("localhost", 2181);
        ecsClient.startZK();
        totalServers = Files.lines(Paths.get(new File("src/app_kvECS/" + configFile).getPath())).count();
        ecsClient.addNodes((int) totalServers,"LRU", 10);
        ecsClient.start();
        this.testEmailData(clientNumbers[index]);
        try {
            new LogSetup("logs/test/Performance.log", Level.DEBUG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private KVStore connectAny() {
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

    public synchronized void testEmailData(int limit) throws Exception {
        totalClients = limit;

        final DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("/home/k/maildir"));
        ArrayList<KVClient> kvClients = new ArrayList<>();

        KVStore defaultKV = connectAny();
        int i = 0;
        int pairLimit = 50;

        for (Path dir : dirStream) {
            HashMap<String, String> keyValues = new HashMap<>();
            Stream<Path> files = Files.walk(dir);
            Iterator it = files.iterator();
            KVClient temp = new KVClient();
            temp.newConnection(defaultKV.getAddress(), defaultKV.getPort());

            int j = 0;
            while (it.hasNext()) {
                String p = it.next().toString();
                if (!new File(p).isDirectory())
                    keyValues.put(p, new String(Files.readAllBytes(Paths.get(p))) );
                if (j++ >= pairLimit)
                    break;
            }
            temp.performanceTest(keyValues, this);
            kvClients.add(temp);
            System.out.println("Started client " + i);
            if (++i >= limit)
                break;
        }
    }



}