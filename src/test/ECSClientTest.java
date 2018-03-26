package test;

import app_kvClient.KVClient;
import app_kvECS.EcsException;
import app_kvServer.IKVServer;
import client.KVStore;
import common.KVMessage;
import ecs.IECSNode;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static test.DSTestSuite.ecsClient;


public class ECSClientTest {

    private KVStore kvClient;

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
    public void tearDown() throws Exception {
        kvClient.disconnect();
    }


    @Test
    public void testServerStopped() throws InterruptedException, EcsException, KeeperException {
        ecsClient.stop();
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

        Assert.assertTrue(ex == null && response.getStatus().equals(KVMessage.StatusType.SERVER_STOPPED));
    }

    @Test
    public void testServerNotResponsible() throws InterruptedException, EcsException, KeeperException {
        ecsClient.start();
        String key = "abd";
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

        Assert.assertTrue(ex == null && response.getStatus().equals(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE));
    }

    @Test
    public void testDeleteNodes() {

        Map<String, IECSNode> nodes = ecsClient.getNodes();
        Assert.assertTrue(nodes.size() == 10);

        List<String> serversToRemove = new ArrayList<>(Arrays.asList("server0"));
        ecsClient.removeNodes(serversToRemove);

        nodes = ecsClient.getNodes();
        Assert.assertTrue(nodes.size() == 9);
    }

    @Test
    public void testSetupNodes() {
        assert (ecsClient.setupNodes(10, IKVServer.CacheStrategy.LFU.getValue(), 55) == null);
    }




}
