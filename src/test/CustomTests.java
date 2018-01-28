package test;

import client.KVStore;
import common.messages.KVMessage;
import common.messages.KVMessage.StatusType;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CustomTests extends TestCase {

    private KVStore kvClient;

    @Test
    public void testPut() {
        String key = "foo2";
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
}