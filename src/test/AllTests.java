package test;

import app_kvECS.ECSClient;
import app_kvECS.EcsException;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;


public class AllTests {

    public static ECSClient ecsClient;

	static {
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

	public static Test suite() {
		TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
		clientSuite.addTestSuite(AdditionalTest.class);
		clientSuite.addTestSuite(ConnectionTest.class);
		clientSuite.addTestSuite(InteractionTest.class);
		return clientSuite;
	}

	public static Test suite2(){
        TestSuite clientSuite = new TestSuite("m2");
        clientSuite.addTestSuite(ConsistentHashTest.class);
        clientSuite.addTestSuite(MetadataTest.class);
        clientSuite.addTestSuite(ECSClientTest.class);
        return clientSuite;
    }

    public static void main(String[] args) throws InterruptedException, EcsException, KeeperException {
        // m1/m2
	    TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
        clientSuite.addTestSuite(AdditionalTest.class);

        TestResult testResult = new TestResult();
        clientSuite.run(testResult);

        Thread.sleep(5000);

        //m2
        clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
        clientSuite.addTestSuite(AdditionalTest.class);

        testResult = new TestResult();
        clientSuite.run(testResult);

        Thread.sleep(5000);

        ecsClient.shutdown();
    }

}
