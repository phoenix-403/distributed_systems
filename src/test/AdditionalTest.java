package test;

import app_kvServer.Persist;
import client.KVStore;
import common.messages.client_server.KVMessage;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static app_kvServer.Persist.init;

public class AdditionalTest extends TestCase {

    private KVStore kvClient;
    private static int CLIENT_CONNECTIONS = 50;

    @Before
    public void setUp() {
        kvClient = new KVStore(null, "localhost", 50000);
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
                KVStore kvClient = new KVStore(null, "localhost", 50000);
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
                KVStore kvClient = new KVStore(null, "localhost", 50000);
                kvClient.connect();
            }

        } catch (Exception e) {
            ex = e;
        }

        assertNull(ex);
    }

    @Test
    public void testPersistGet() {
        String key = "testPersistGet";
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
        String key = "testPersistUpdate";
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
        String key = "testDoubleUpdate";
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
        String key = "testGetNonExistant";
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
        String key = "testGetAfterDelete";
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
        String key = "testDoubleDelete";
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

        Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
    }

    @Test
    public void testDoubleDelete2() {
        String key = "testDoubleDelete2";
        String value ="deleteThisTwice";
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
    public void testDeleteNonExistant() {
        String key = "testDeleteNonExistant";
        String value = "";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value);
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
    }

    @Test
    public void testDisconnectedPut() {
        String key = "testDisconnectedPut";
        String value = "123";
        Exception ex = null;

        try {
            kvClient.disconnect();
            kvClient.put(key, value);
            kvClient.connect();
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex.getMessage().equals("Not Connected"));
    }



    @Test
    public void testKeyFileLookUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // testing private method
        assert (init(""));

        Method method = Persist.class.getDeclaredMethod("getFileKeyStoredIn", String.class);
        method.setAccessible(true);

        Assert.assertEquals("97.db", ((File) method.invoke(null, "ABC")).getName());
        Assert.assertEquals("97.db", ((File) method.invoke(null, "abc")).getName());
        Assert.assertEquals("98.db", ((File) method.invoke(null, "b")).getName());
        Assert.assertEquals("99.db", ((File) method.invoke(null, "c")).getName());
        Assert.assertEquals("123.db", ((File) method.invoke(null, "~")).getName());

    }

}
