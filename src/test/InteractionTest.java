package test;

import app_kvClient.KVClient;
import app_kvECS.ECSClient;
import app_kvECS.EcsException;
import client.KVStore;
import common.KVMessage;
import common.KVMessage.StatusType;
import junit.framework.TestCase;
import org.apache.zookeeper.KeeperException;
import org.junit.*;

import java.io.IOException;


public class InteractionTest extends TestCase {

    private ECSClient ecsClient;
    private KVStore kvClient;

    @BeforeClass
    public void setUp() {
        try {
            ecsClient = new ECSClient("ecs.config");
            ecsClient.startZK();
            ecsClient.addNodes(10, "LRU", 10);
            ecsClient.start();
            Thread.sleep(1000);
        } catch (IOException | EcsException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
        KVClient client = new KVClient();
        kvClient = new KVStore(client, "localhost", 50009);
        try {
            kvClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws InterruptedException, KeeperException, EcsException {
        ecsClient.shutdown();
        ecsClient.stopZK();
        kvClient.disconnect();
    }

    @Test
    public void testPut() {
        String key = "ab";
        String value = "bar2";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.PUT_SUCCESS);
    }

    @Test
    public void testPutDisconnected() {
        kvClient.disconnect();
        String key = "ab";
        String value = "bar";
        Exception ex = null;

        try {
            kvClient.put(key, value);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertNotNull(ex);
    }

    @Test
    public void testUpdate() {
        String key = "ab";
        String initialValue = "initial";
        String updatedValue = "updated";

        KVMessage response = null;
        Exception ex = null;

        try {
            kvClient.put(key, initialValue);
            response = kvClient.put(key, updatedValue);

        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.PUT_UPDATE
                && response.getValue().equals(updatedValue));
    }

    @Test
    public void testDelete() {
        String key = "ab";
        String value = "toDelete";

        KVMessage response = null;
        Exception ex = null;

        try {
            kvClient.put(key, value);
            response = kvClient.put(key, null);

        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.DELETE_SUCCESS);
    }

    @Test
    public void testDelete2() {
        String key = "ab";
        String value = "toDelete";

        KVMessage response = null;
        Exception ex = null;

        try {
            kvClient.put(key, value);
            response = kvClient.put(key, "");

        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.DELETE_SUCCESS);
    }

    @Test
    public void testGet() {
        String key = "ab";
        String value = "bar";
        KVMessage response = null;
        Exception ex = null;

        try {
            kvClient.put(key, value);
            response = kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getValue().equals("bar"));
    }

    @Test
    public void testGetUnsetValue() throws IOException {
        String key = "ab";
        kvClient.put(key, null);
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.GET_ERROR);
    }


}
