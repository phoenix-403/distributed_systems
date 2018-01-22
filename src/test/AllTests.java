package test;

import java.io.IOException;

import org.apache.log4j.Level;

import main.app_kvServer.KVServer;
import junit.framework.Test;
import junit.framework.TestSuite;
import main.logger.LogSetup;


public class AllTests {

	static {
		try {
			new LogSetup("logs/test/test.log", Level.ERROR);
			new KVServer(50000, 10, "FIFO");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static Test suite() {
		TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
		clientSuite.addTestSuite(ConnectionTest.class);
		clientSuite.addTestSuite(InteractionTest.class); 
		clientSuite.addTestSuite(AdditionalTest.class); 
		return clientSuite;
	}
	
}
