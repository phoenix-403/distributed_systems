package common.helper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZkConnector {

    private ZooKeeper zooKeeper;
    private CountDownLatch countDownLatch = new CountDownLatch(1);


    public ZooKeeper connect(String host) throws IOException, InterruptedException {
        zooKeeper = new ZooKeeper(host, 30000, new Watcher() {
            public void process(WatchedEvent watchedEvent) {
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    countDownLatch.countDown();
                }
            }
        });
        countDownLatch.await();
        return zooKeeper;
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }
}
