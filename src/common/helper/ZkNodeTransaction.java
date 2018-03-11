package common.helper;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;


public class ZkNodeTransaction {

    private static Logger logger = LogManager.getLogger(ZkNodeTransaction.class);
    private ZooKeeper zooKeeper;

    public ZkNodeTransaction(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;

        try {
            new LogSetup("ds_data/transactions.log", Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String createZNode(String path, byte[] data, CreateMode createMode) throws KeeperException,
            InterruptedException {
        //logger.info("creating znode " + path);
        return zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
    }

    public byte[] read(String path) throws KeeperException, InterruptedException {
        //logger.info("reading znode " + path);
        return zooKeeper.getData(path, true, zooKeeper.exists(path, true));
    }


    public void write(String path, byte[] data) throws KeeperException, InterruptedException {
        //logger.info("writing znode " + path + new String(data));
        zooKeeper.setData(path, data, zooKeeper.exists(path, true).getVersion());
    }

    public void delete(String path) throws KeeperException, InterruptedException {

        if (!path.equals("/zookeeper")) {
            List<String> children = zooKeeper.getChildren(path, false);
            if (children.size() > 0) {
                for (String child : children) {
                    if (path.equals("/")) {
                        delete(path + child);
                    } else {
                        delete(path + "/" + child);
                    }
                }
            }
            if (!path.equals("/")) {
                zooKeeper.delete(path, zooKeeper.exists(path, true).getVersion());
                logger.info("deleted: " + path);
            }
        }


    }
}

