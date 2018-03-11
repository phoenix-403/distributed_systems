//package test;
//
//import app_kvClient.KVClient;
//import app_kvECS.ECSClient;
//import app_kvECS.EcsException;
//import client.KVStore;
//import ecs.IECSNode;
//import org.apache.zookeeper.KeeperException;
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.io.IOException;
//import java.util.Collection;
//import java.util.Map;
//
//public class ECSClientTest {
//    private ECSClient ecsClient;
//    private KVStore kvClient;
//    private static int CLIENT_CONNECTIONS = 50;
//
//    @BeforeClass
//    public void setUp() {
//        try {
//            ecsClient = new ECSClient("ecs.config");
//            ecsClient.startZK();
//            ecsClient.addNodes(10, "LRU", 10);
//            Thread.sleep(1000);
//        } catch (IOException | EcsException | InterruptedException | KeeperException e) {
//            e.printStackTrace();
//        }
//        KVClient client = new KVClient();
//        kvClient = new KVStore(client, "localhost", 50009);
//        try {
//            kvClient.connect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @After
//    public void tearDown() throws InterruptedException, KeeperException, EcsException {
//        ecsClient.shutdown();
//        ecsClient.stopZK();
//        kvClient.disconnect();
//    }
//
//
//
//    @Test
//    public void testAddNodes() {
//        Collection<IECSNode> nodes = null;
//        Exception ex = null;
//
//        try {
//            setUp();
//            nodes = ecsClient.addNodes(numberOfNodes, strategy, cacheSize);
//            for (IECSNode node: nodes) {
//                // try connecting to the nodes
//            }
//            tearDown();
//        } catch (Exception e) {
//            ex = e;
//        }
//
//        Assert.assertTrue(ex == null); //needs to be changed
//    }
//
//    @Test
//    public void testAwaitNodes() {
//        boolean success = false;
//        Exception ex = null;
//
//        try {
//            setUp();
//            ecsClient.addNodes(numberOfNodes, strategy, cacheSize);
//            success = ecsClient.awaitNodes(numberOfNodes, timeout);
//            tearDown();
//        } catch (Exception e) {
//            ex = e;
//        }
//
//        Assert.assertTrue(ex == null && success);
//    }
//
//    @Test
//    public void testGetNodes() {
//        Map<String, IECSNode> nodes = null;
//        Exception ex = null;
//
//        try {
//            setUp();
//            nodes = ecsClient.getNodes();
//            tearDown();
//        } catch (Exception e) {
//            ex = e;
//        }
//
//        Assert.assertTrue(ex == null && nodes != null); //needs to be changed
//    }
//
//    @Test
//    public void testGetNodeByKey() {
//        IECSNode node = null;
//        Exception ex = null;
//
//        try {
//            setUp();
//            node = ecsClient.getNodeByKey(nodeID);
//            tearDown();
//        } catch (Exception e) {
//            ex = e;
//        }
//        Assert.assertTrue(ex == null && node != null); //needs to be changed
//    }
//
//}
