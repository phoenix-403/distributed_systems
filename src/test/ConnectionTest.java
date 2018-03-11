package test;

import app_kvECS.ECSClient;
import app_kvECS.EcsException;
import client.KVStore;
import junit.framework.TestCase;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.UnknownHostException;


public class ConnectionTest extends TestCase {

	private ECSClient ecsClient;

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
	}

	
	public void testConnectionSuccess() {
		
		Exception ex = null;
		
		KVStore kvClient = new KVStore(null, "localhost", 50009);
		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}	
		
		assertNull(ex);
	}
	
	
	public void testUnknownHost() {
		Exception ex = null;
		KVStore kvClient = new KVStore(null, "unknown", 50000);
		
		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e; 
		}
		
		assertTrue(ex instanceof UnknownHostException);
	}
	
	
	public void testIllegalPort() {
		Exception ex = null;
		KVStore kvClient = new KVStore(null, "localhost", 123456789);
		
		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e; 
		}
		
		assertTrue(ex instanceof IllegalArgumentException);
	}


    @After
    public void tearDown() throws InterruptedException, KeeperException, EcsException {
        ecsClient.shutdown();
        ecsClient.stopZK();
    }
	
}

