package test;

import app_kvClient.KVClient;
import app_kvECS.EcsException;
import client.KVStore;
import common.KVMessage;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class AdditionalTest {


    private KVStore kvClient;
    private int CLIENT_CONNECTIONS = 10;

    @Before
    public void setUp() {
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
        kvClient.disconnect();
    }

    @Test
    public void testReconnect() {
        Exception ex = null;

        try {
            for (int i = 0; i < CLIENT_CONNECTIONS; i++) {
                KVStore kvClient = new KVStore(null, "localhost", 50009);
                kvClient.disconnect();
                kvClient.connect();
            }

        } catch (Exception e) {
            ex = e;
        }

        Assert.assertNull(ex);
    }

    @Test
    public void testMultipleConnections() {
        Exception ex = null;

        try {
            for (int i = 0; i < CLIENT_CONNECTIONS; i++) {
                KVStore kvClient = new KVStore(null, "localhost", 50009);
                kvClient.connect();
            }

        } catch (Exception e) {
            ex = e;
        }

        Assert.assertNull(ex);
    }

    @Test
    public void testPersistGet() {
        String key = "ab";
        String value = "persistPls";
        KVMessage response = null;
        Exception ex = null;
        try {
            response = kvClient.put(key, value);
            kvClient.disconnect();
            kvClient.connect();
            response = kvClient.get(key);
        } catch (Exception e1) {
            ex = e1;
        }

        Assert.assertTrue(ex == null && response.getValue().equals(value));
    }

    @Test
    public void testPersistUpdate() {
        String key = "ab";
        String value1 = "doNotPersist";
        String value2 = "persistPls";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value1);
            response = kvClient.put(key, value2);
            kvClient.disconnect();
            kvClient.connect();
            response = kvClient.get(key);
        } catch (Exception e1) {
            ex = e1;
        }

        Assert.assertTrue(ex == null && response.getValue().equals(value2));
    }

    @Test
    public void testDoubleUpdate() {
        String key = "ab";
        String value1 = "one";
        String value2 = "two";
        String value3 = "three";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value1);
            response = kvClient.put(key, value2);
            response = kvClient.put(key, value3);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getValue().equals(value3));
    }

    @Test
    public void testGetNonExistent() throws IOException {
        String key = "ab";
        kvClient.put(key, null);
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.GET_ERROR);
    }

    @Test
    public void testGetAfterDelete() {
        String key = "ab";
        String value = "init";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
            response = kvClient.put(key, "");
            response = kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.GET_ERROR);
    }

    @Test
    public void testDoubleDelete() {
        String key = "ab";
        String value = "deleteThisTwice";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
            response = kvClient.put(key, "");
            response = kvClient.put(key, "");
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
    }

    @Test
    public void testDoubleDelete2() {
        String key = "ab";
        String value = "deleteThisTwice";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
            response = kvClient.put(key, null);
            response = kvClient.put(key, null);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
    }

    @Test
    public void testDeleteNonExistent() {
        String key = "ab";
        String value = "";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
            response = kvClient.put(key, value);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
    }



}
