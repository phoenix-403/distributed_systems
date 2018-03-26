package test;

import app_kvClient.KVClient;
import app_kvServer.IKVServer;
import app_kvServer.KVServer;
import client.KVStore;
import org.junit.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ReplicaTest {

    private KVServer localServer;
    private Thread localServerThread;
    private KVStore localKvClient;
    private KVStore ecsKvClient;

    private void restartLocalServer() throws Exception {
        localServer.closeTest();

        // cant restart a closed thread
        localServer = new KVServer("test", "localhost", 2181);
        localServer.initKVServer(1234, 11, IKVServer.CacheStrategy.LFU.getValue());
        localServerThread = new Thread(localServer);
        localServerThread.start();
    }

    @Before
    public void setUp() throws Exception {
        // setting up local server
        localServer = new KVServer("test", "localhost", 2181);
        localServer.initKVServer(1234, 11, IKVServer.CacheStrategy.LFU.getValue());
        localServerThread = new Thread(localServer);
        localServerThread.start();

        KVClient client = new KVClient();
        localKvClient = new KVStore(client, "localhost", 1234);
        localKvClient.connect();

        // setting up ecs
        client = new KVClient();
        ecsKvClient = new KVStore(client, "localhost", 50000);
        ecsKvClient.connect();

    }

    @After
    public void tearDown() {
        // closing local server
        localServer.closeTest();
        localKvClient.disconnect();

        // closing ecs client
        ecsKvClient.disconnect();
    }

    @Test
    public void testDB() {
        File file = new File("ds_data/test/db/data.db");
        assert (file.exists());
    }

    @Test
    public void testReplicaDB() {
        File file = new File("ds_data/test/db/dataREP.db");
        assert (file.exists());
    }


    @Test
    public void testShutDown() throws Exception {
        // data shouldn't persist on file, it should be in zookeeper instead
        FileWriter fileWriter = new FileWriter("ds_data/test/db/data.db", true);
        fileWriter.write("some jiberish!");
        fileWriter.close();

        restartLocalServer();
        File file = new File("ds_data/test/db/data.db");
        Assert.assertTrue(file.length() == 0);
    }


    @Test
    public void testShutDownRep() throws Exception {
        // data shouldn't persist on file, it should be in zookeeper instead
        FileWriter fileWriter = new FileWriter("ds_data/test/db/dataREP.db", true);
        fileWriter.write("some jiberish!");
        fileWriter.close();

        restartLocalServer();
        File file = new File("ds_data/test/db/dataREP.db");
        Assert.assertTrue(file.length() == 0);
    }



    @Test
    public void testReplicate() throws InterruptedException, IOException {
        ecsKvClient.put("localhost:50000", "2323");
        Thread.sleep(30000);
        File file1 = new File(System.getProperty("user.home") + "/ds_data/server3/db/dataREP.db");
        File file2 = new File(System.getProperty("user.home") + "/ds_data/server6/db/dataREP.db");
        Assert.assertTrue(file1.length() != 0 && file2.length() != 0);

    }


}
