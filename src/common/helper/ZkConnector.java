package common.helper;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class ZkConnector {

    private ZooKeeper zooKeeper;
    private CountDownLatch countDownLatch = new CountDownLatch(1);


    public ZooKeeper connect(String host) throws IOException, InterruptedException {
        zooKeeper = new ZooKeeper(host, 30000, watchedEvent -> {
            if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        Logger.getLogger("org.apache.zookeeper.ZooKeeper").setLevel(Level.ERROR);
        return zooKeeper;
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }
}
