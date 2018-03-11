//    package test;
//
//    import app_kvECS.ECSClient;
//    import app_kvECS.EcsException;
//    import app_kvServer.KVServer;
//    import app_kvServer.Persist;
//    import client.KVStore;
//    import common.messages.client_server.KVMessage;
//    import ecs.ECSNode;
//    import junit.framework.TestCase;
//    import org.apache.zookeeper.KeeperException;
//    import org.junit.After;
//    import org.junit.Assert;
//    import org.junit.Before;
//    import org.junit.Test;
//
//    import java.io.File;
//    import java.io.IOException;
//    import java.lang.reflect.InvocationTargetException;
//    import java.lang.reflect.Method;
//    import java.nio.file.Files;
//    import java.util.ArrayList;
//    import java.util.regex.Pattern;
//
//    import static app_kvServer.Persist.init;
//
//    public class AdditionalTest extends TestCase {
//
//        private ECSClient ecsClient;
//        String[] args = new String[]{"server18", "localhost", "2181", "54416", "10", "LRU"};
//        private KVServer kvServer;
//        private static int CLIENT_CONNECTIONS = 50;
//
//        @Before
//        public void setUp() {
//            try {
////                ecsClient = new ECSClient("ecs.config");
//                kvServer = new KVServer(args[0], args[1], Integer.parseInt(args[2]));
//                kvServer.initKVServer(Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
//                Thread thread = new Thread(kvServer);
//                thread.start();
//
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            } catch (EcsException e1) {
//                e1.printStackTrace();
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//        }
//
//        @After
//        public void tearDown() {
//            kvServer.close();
//        }
//
//        @Test
//        public void testReconnect() {
//            Exception ex = null;
//
//            try {
//                for (int i=0; i < CLIENT_CONNECTIONS; i++) {
//                    KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                    kvClient.connect();
//                    kvClient.disconnect();
//                    kvClient.connect();
//                }
//
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            assertNull(ex);
//        }
//
//        @Test
//        public void testMultipleConnections() {
//            Exception ex = null;
//
//            try {
//                for (int i=0; i < CLIENT_CONNECTIONS; i++) {
//                    KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                    kvClient.connect();
//                }
//
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            assertNull(ex);
//        }
//
//        @Test
//        public void testPersistGet() {
//            String key = "testPersistGet";
//            String value = "persistPls";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.put(key, value);
//
//                kvClient.disconnect();
//                kvClient.connect();
//                response = kvClient.get(key);
//                kvClient.disconnect();
//            } catch (Exception e1) {
//                ex = e1;
//            }
//
//            Assert.assertTrue(ex == null && response.getValue().equals(value));
//        }
//
//        @Test
//        public void testPersistUpdate() {
//            String key = "testPersistUpdate";
//            String value1 = "doNotPersist";
//            String value2 = "persistPls";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.put(key, value1);
//                response = kvClient.put(key, value2);
//                kvClient.disconnect();
//                kvClient.connect();
//                response = kvClient.get(key);
//                kvClient.disconnect();
//            } catch (Exception e1) {
//                ex = e1;
//            }
//
//            Assert.assertTrue(ex == null && response.getValue().equals(value2));
//        }
//
//        @Test
//        public void testDoubleUpdate() {
//            String key = "testDoubleUpdate";
//            String value1 = "one";
//            String value2 = "two";
//            String value3 = "three";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.put(key, value1);
//                response = kvClient.put(key, value2);
//                response = kvClient.put(key, value3);
//                kvClient.disconnect();
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            Assert.assertTrue(ex == null && response.getValue().equals(value3));
//        }
//
//        @Test
//        public void testGetNonExistant() {
//            String key = "testGetNonExistant";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.get(key);
//                kvClient.disconnect();
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.GET_ERROR);
//        }
//
//        @Test
//        public void testGetAfterDelete() {
//            String key = "testGetAfterDelete";
//            String value = "init";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.put(key, value);
//                response = kvClient.put(key, "");
//                response = kvClient.get(key);
//                kvClient.disconnect();
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.GET_ERROR);
//        }
//
//        @Test
//        public void testDoubleDelete() {
//            String key = "testDoubleDelete";
//            String value ="deleteThisTwice";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.put(key, value);
//                response = kvClient.put(key, "");
//                response = kvClient.put(key, "");
//                kvClient.disconnect();
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
//        }
//
//        @Test
//        public void testDoubleDelete2() {
//            String key = "testDoubleDelete2";
//            String value ="deleteThisTwice";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.put(key, value);
//                response = kvClient.put(key, null);
//                response = kvClient.put(key, null);
//                kvClient.disconnect();
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
//        }
//
//        @Test
//        public void testDeleteNonExistant() {
//            String key = "testDeleteNonExistant";
//            String value = "";
//            KVMessage response = null;
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                response = kvClient.put(key, value);
//                kvClient.disconnect();
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            Assert.assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
//        }
//
//        @Test
//        public void testDisconnectedPut() {
//            String key = "testDisconnectedPut";
//            String value = "123";
//            Exception ex = null;
//
//            try {
//                KVStore kvClient = new KVStore(null, args[1], Integer.parseInt(args[2]));
//                kvClient.connect();
//                kvClient.disconnect();
//                kvClient.put(key, value);
//            } catch (Exception e) {
//                ex = e;
//            }
//
//            Assert.assertTrue(ex.getMessage().equals("Not Connected"));
//        }
//
//
//
//        @Test
//        public void testKeyFileLookUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//
//            // testing private method
//            assert (init(""));
//
//            Method method = Persist.class.getDeclaredMethod("getFileKeyStoredIn", String.class);
//            method.setAccessible(true);
//
//            Assert.assertEquals("97.db", ((File) method.invoke(null, "ABC")).getName());
//            Assert.assertEquals("97.db", ((File) method.invoke(null, "abc")).getName());
//            Assert.assertEquals("98.db", ((File) method.invoke(null, "b")).getName());
//            Assert.assertEquals("99.db", ((File) method.invoke(null, "c")).getName());
//            Assert.assertEquals("123.db", ((File) method.invoke(null, "~")).getName());
//
//        }
//
//    }
