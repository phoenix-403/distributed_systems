package test;

import app_kvClient.KVClient;
import app_kvECS.EcsException;
import app_kvServer.IKVServer;
import app_kvServer.KVServer;
import client.KVStore;
import junit.framework.TestCase;
import org.apache.zookeeper.KeeperException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ReplicaTest extends TestCase {

    private KVServer server;
    private KVStore kvClient;
//    private KVStore kvClient2;
    private KVStore kvClient3;
    private KVStore kvClientTest;
    private KVServer serverTest;
    private Thread thread;
    @BeforeClass
    public void setUp() throws Exception {
//        serverTest = new KVServer("test","localhost",9999);
//        serverTest.initKVServer(1234,11, IKVServer.CacheStrategy.LFU.getValue());
//        thread = new Thread(serverTest);
//        thread.start();

        server = new KVServer("test","localhost",2181);
        server.initKVServer(1234,11, IKVServer.CacheStrategy.LFU.getValue());
        thread = new Thread(server);
        thread.start();

        KVClient client = new KVClient();
        kvClient = new KVStore(client, "localhost", 1234);
//        KVClient client2 = new KVClient();
//        kvClient2 = new KVStore(client2, "localhost", 1235);
//        KVClient client3 = new KVClient();
//        kvClient3 = new KVStore(client3, "localhost", 50000);
//        KVClient testClient = new KVClient();
//        kvClientTest = new KVStore(testClient,"localhost",2182);

        try {
            kvClient.connect();
//            kvClient2.connect();
//            kvClient3.connect();
//            kvClientTest.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public void tearDown() throws InterruptedException, KeeperException, EcsException {
        thread.stop();
//        kvClient.disconnect();
//        kvClient2.disconnect();
//        kvClient3.disconnect();
//        kvClientTest.disconnect();
//        serverTest.close();
    }

    @Test
    public void testDB() throws Exception {
        File file = new File("./ds_data/test/db/data.db");
        assert(file.exists());

    }

    @Test
    public void testReplicaDB() throws Exception {
//        KVServer server = new KVServer("test","localhost",2181);
//        server.initKVServer(1234,11, IKVServer.CacheStrategy.LFU.getValue());
//        Thread thread = new Thread(server);
//        thread.start();


        File file = new File("./ds_data/test/db/dataREP.db");
        assert(file.exists());

    }

    // actually replicas will still have the data.. i never bothwer to delete it cause on the next server start it deletes the entire file
    @Test
    public void testShutDown() throws Exception {
        // data shouldn't persist on file, it should be in zookeeper instead
        KVServer server2 = new KVServer("test2","localhost",9999);
        server2.initKVServer(10235,11, IKVServer.CacheStrategy.LFU.getValue());
        Thread thread1 = new Thread(server2);
        thread1.start();
        KVClient client2 = new KVClient();
        KVStore kvClient2 = new KVStore(client2, "localhost", 1235);
        kvClient2.connect();
        kvClient2.put("localhost:test2","1234");
        thread1.stop();

        Thread threadRe = new Thread(server2);
        threadRe.start();
        File file = new File("./ds_data/test2/db/data.db");
        assertTrue(file.length() == 0);
    }

    // actually replicas will still have the data.. i never bothwer to delete it cause on the next server start it deletes the entire file
    @Test
    public void testShutDownRep() throws Exception {
        // data shouldn't persist on file, it should be in zookeeper instead
        KVServer server = new KVServer("test2","localhost",2182);
        server.initKVServer(1234,11, IKVServer.CacheStrategy.LFU.getValue());
        Thread thread = new Thread(server);
        thread.start();
        kvClientTest.put("lo","2222");
        thread.stop();

        Thread threadRe = new Thread(server);
        threadRe.start();
        File file = new File(System.getProperty("user.home") + "ds_data/test2/db/dataREP.db");
        assertTrue(file.length() == 0);
    }

    @Test
    public void testReplicate() throws InterruptedException, IOException {
        kvClient.put("localhost:server1","2323");
        Thread.sleep(30000);
        File file1 = new File(System.getProperty("user.home") + "ds_data/server0/db/dataREP.db");
        File file2 = new File(System.getProperty("user.home") + "ds_data/server9/db/dataREP.db");
        assertTrue(file1.length() != 0 && file2.length() != 0);

    }





}


