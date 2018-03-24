package test;

import app_kvECS.ECSClient;
import app_kvECS.EcsException;
import org.apache.zookeeper.KeeperException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses({AdditionalTest.class, ConnectionTest.class, InteractionTest.class, ConsistentHashTest.class,
        ECSClientTest.class, MetadataTest.class})
public class DSTestSuite {


    public static ECSClient ecsClient;

    @BeforeClass
    public static void doYourOneTimeSetup() throws IOException, EcsException, KeeperException, InterruptedException {
        ecsClient = new ECSClient("localhost", 2181);
        ecsClient.startZK();
        ecsClient.addNodes(10, "LRU", 10);
        ecsClient.start();
        Thread.sleep(1000);
    }

    @AfterClass
    public static void doYourOneTimeTeardown() throws InterruptedException, EcsException, KeeperException {
        ecsClient.shutdown();
    }
}
