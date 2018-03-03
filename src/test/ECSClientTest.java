package test;

import app_kvECS.EcsException;
import client.KVStore;
import app_kvECS.ECSClient;
import common.messages.KVMessage;
import common.messages.KVMessage.StatusType;
import ecs.IECSNode;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class ECSClientTest {
    private String host = "chenhen7@ug157.eecg.utoronto.ca";
    private int cacheSize = 10;
    private String strategy = "LRU";
    private String nodeID = "";
    private int numberOfNodes = 10;
    private ECSClient ecsClient;
    private int timeout = 60000;

    public void setUp() {
        Exception ex = null;
        try {
            ecsClient = new ECSClient("ecs.config");
        } catch (Exception e) {
            ex = e;
        }
    }

    public void tearDown() {
        ecsClient.shutdown();
    }

    @Test
    public void testSetupNodes() {
        Collection<IECSNode> nodes = null;
        Exception ex = null;

        try {
            setUp();
            nodes = ecsClient.setupNodes(numberOfNodes, strategy, cacheSize);
            tearDown();
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertTrue(ex == null && nodes != null); //needs to be changed
    }

    @Test
    public void testAddNodes() {
        Collection<IECSNode> nodes = null;
        Exception ex = null;

        try {
            setUp();
            nodes = ecsClient.addNodes(numberOfNodes, strategy, cacheSize);
            for (IECSNode node: nodes) {
                // try connecting to the nodes
            }
            tearDown();
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && nodes != null); //needs to be changed
    }

    @Test
    public void testAwaitNodes() {
        boolean success = false;
        Exception ex = null;

        try {
            setUp();
            ecsClient.addNodes(numberOfNodes, strategy, cacheSize);
            success = ecsClient.awaitNodes(numberOfNodes, timeout);
            tearDown();
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && success  == true);
    }

    @Test
    public void testGetNodes() {
        Map<String, IECSNode> nodes = null;
        Exception ex = null;

        try {
            setUp();
            nodes = ecsClient.getNodes();
            tearDown();
        } catch (Exception e) {
            ex = e;
        }

        Assert.assertTrue(ex == null && nodes != null); //needs to be changed
    }

    @Test
    public void testGetNodeByKey() {
        IECSNode node = null;
        Exception ex = null;

        try {
            setUp();
            node = ecsClient.getNodeByKey(nodeID);
            tearDown();
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertTrue(ex == null && node != null); //needs to be changed
    }

}
