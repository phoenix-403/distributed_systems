package test;

import client.KVStore;
import common.messages.KVMessage;
import common.messages.KVMessage.StatusType;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CustomTest extends TestCase {

    private KVStore kvClient;
    private static int CLIENT_CONNECTIONS = 50;

    @Before
    public void setUp() {
        kvClient = new KVStore("localhost", 50000);
        try {
            kvClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        kvClient.disconnect();
    }

    @Test
    public void testReconnect() {
        Exception ex = null;

        try {
            for (int i=0; i < CLIENT_CONNECTIONS; i++) {
                KVStore kvClient = new KVStore("localhost", 50000);
                kvClient.disconnect();
                kvClient.connect();
            }

        } catch (Exception e) {
            ex = e;
        }

        assertNull(ex);
    }

    @Test
    public void testMultipleConnections() {
        Exception ex = null;

        try {
            for (int i=0; i < CLIENT_CONNECTIONS; i++) {
                KVStore kvClient = new KVStore("localhost", 50000);
                kvClient.connect();
            }

        } catch (Exception e) {
            ex = e;
        }

        assertNull(ex);
    }

    @Test
    public void testPersistGet() {
        String key = "updateThisTwice";
        String value = "persistPls";
        KVMessage response = null;
        Exception ex = null;

        try {
            kvClient.put(key, value);
            kvClient.disconnect();
            kvClient.connect();
            response = kvClient.get(key);
        } catch (Exception e1) {
            ex = e1;
        }

        Assert.assertTrue(ex == null && response.getValue().equals(value));
    }

    @Test
    public void testDoubleUpdate() {
        String key = "updateThisTwice";
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
    public void testGetNonExistant() {
        String key = "thisDoesNotExist";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.GET_ERROR);
    }

    @Test
    public void testGetAfterDelete() {
        String key = "deleteAndGet";
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

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.GET_ERROR);
    }

    @Test
    public void testDoubleDelete() {
        String key = "thisDoesNotExist";
        String value ="deleteThisTwice";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
            response = kvClient.put(key, "");
            response = kvClient.put(key, "");
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.DELETE_ERROR);
    }

    @Test
    public void testDeleteNonExistant() {
        String key = "thisDoesNotExist";
        String value = "";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == StatusType.DELETE_ERROR);
    }

    @Test
    public void testDisconnectedGet() {
        String key = "thisShouldNotWork";
        Exception ex = null;

        try {
            kvClient.disconnect();
            kvClient.get(key);
            kvClient.connect();
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex.getMessage().equals("Not Connected"));
    }

    @Test
    public void testDisconnectedPut() {
        String key = "thisShouldNotWork";
        String value = "123";
        KVMessage response = null;
        Exception ex = null;

        try {
            kvClient.disconnect();
            response = kvClient.put(key, value);
            kvClient.connect();
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex.getMessage().equals("Not Connected"));
    }


}