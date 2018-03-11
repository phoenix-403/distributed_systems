package test;

import app_kvClient.KVClient;
import app_kvECS.ECSClient;
import app_kvECS.EcsException;
import client.KVStore;
import ecs.ECSNode;
import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
    String configFile;
    long averageTime=0;
    long totalSampleTime=0;
    long sample_count=1;
    private static Logger logger = LogManager.getLogger(PerformanceTest.class);

    public static void main(String[] args) throws Exception {
        ECSClient ecsClient = new ECSClient(args[0]);
        ecsClient.startZK();
        int serverCount = (int) Files.lines(Paths.get(new File("src/app_kvECS/" + args[0]).getPath())).count();
        ecsClient.addNodes(serverCount,"LRU", 10);
        ecsClient.start();
        PerformanceTest test = new PerformanceTest(args[0]);
        test.testRead(Integer.parseInt(args[1]));
    }

    public void updateAverage(long time){
        totalSampleTime+=time;
        averageTime=totalSampleTime/sample_count;
        sample_count++;
        logger.debug("Average time is " + averageTime);
    }

    PerformanceTest(String configFile){
        this.configFile = configFile;
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

    public void testRead(int limit) throws Exception {

        final DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("/home/k/maildir"));
        ArrayList<KVClient> kvClients = new ArrayList<>();

        KVStore defaultKV = connectAny();
        long time = 0;
        int i = 0;
        int pairLimit = 20;

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
            if (i++ >= limit)
                break;
        }
    }



}