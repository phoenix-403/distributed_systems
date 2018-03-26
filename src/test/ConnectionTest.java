package test;

import app_kvClient.KVClient;
import app_kvECS.EcsException;
import client.KVStore;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ConnectionTest {

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
    public void tearDown() throws InterruptedException, KeeperException, EcsException {
        kvClient.disconnect();
    }

	@Test
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

	@Test
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

	@Test
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
	
}

