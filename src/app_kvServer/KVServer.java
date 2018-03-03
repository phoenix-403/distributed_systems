package app_kvServer;

import common.helper.ZkConnector;
import common.helper.ZkNodeTransaction;
import ecs.ZkStructureNodes;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class KVServer implements IKVServer, Runnable {

    private static Logger logger = LogManager.getLogger(KVServer.class);

    private String name;
    private int port;
    private int cacheSize;
    private CacheStrategy cacheStrategy;

    private InetAddress inetAddress;

    private ServerSocket serverSocket;
    private boolean serverRunning;
    private boolean acceptingRequests;

    private ZkConnector zkConnector;
    private ZooKeeper zooKeeper;
    private ZkNodeTransaction zkNodeTransaction;


    private List<Thread> clientThreads;

    /**
     * Start KV Server with selected name
     *
     * @param name       unique name of server
     * @param zkHostname hostname where zookeeper is running
     * @param zkPort     port where zookeeper is running
     */
    public KVServer(String name, String zkHostname, int zkPort) throws IOException, InterruptedException {
        this.name = name;

        // connect to zoo keeper
        zkConnector = new ZkConnector();
        zooKeeper = zkConnector.connect(zkHostname + ":" + zkPort);
        zkNodeTransaction = new ZkNodeTransaction(zooKeeper);
    }

    /**
     * Start KV Server at given port
     *
     * @param port      given port for storage server to operate
     * @param metadata  initial metadata
     * @param cacheSize specifies how many key-value pairs the server is allowed
     *                  to keep in-memory
     * @param strategy  specifies the cache replacement strategy in case the cache
     *                  is full and there is a GET- or PUT-request on a key that is
     *                  currently not contained in the cache. Options are "FIFO", "LRU",
     *                  and "LFU".
     */
    private void initKVServer(int port, String metadata, int cacheSize, String strategy) {

        try {
            new LogSetup("logs/server/server.log", Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // saving server variables
        this.port = port;
        this.cacheSize = cacheSize;
        this.cacheStrategy = CacheStrategy.valueOf(strategy);

        // getting host info
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            logger.error("Unknown host exception:\r\n" + e.getMessage());
        }

        // Initializing the server
        logger.info("Attempting to initialize server...");
        // setting up cache
        Cache.setup(cacheSize, cacheStrategy);
        // setting up Database
        if (!Persist.init()) {
            logger.fatal("Can't start a server without a database!");
            // if persist is not available exit server.. cant live without persist but can live without cache
            System.exit(-1);
        }
        serverRunning = false;
        acceptingRequests = false;
        clientThreads = new ArrayList<>();
        try {
            serverSocket = new ServerSocket(port);
            if (port == 0) {
                this.port = serverSocket.getLocalPort();
            }
            logger.info("Initialized! listening on port: " + serverSocket.getLocalPort());

            // adding hb node
            zkNodeTransaction.createZNode(ZkStructureNodes.HEART_BEAT.getValue() + "/" + name, null, CreateMode.EPHEMERAL);

        } catch (IOException e) {
            logger.error("Error! Cannot open server socket: " + e.getMessage());
        } catch (InterruptedException | KeeperException e) {
            logger.error("Server " + name + " was not added to HB in zookeeper !!!");
        }


    }

    public boolean isAcceptingRequests() {
        return acceptingRequests;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHostname() {
        return inetAddress != null ? inetAddress.getHostName() : null;
    }

    @Override
    public CacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    @Override
    public int getCacheSize() {
        return cacheSize;
    }

    @Override
    public boolean inStorage(String key) {
        try {
            return Persist.checkIfExists(key);
        } catch (IOException e) {
            logger.error("Can't check if " + key + " exists in storage: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean inCache(String key) {
        return Cache.inCache(key);
    }

    @Override
    public String getKV(String key) throws IOException {
        return Cache.lookup(key);
    }

    @Override
    public void putKV(String key, String value) throws IOException {
        putKVWithError(key, value);
    }

    public boolean putKVWithError(String key, String value) throws IOException {
        return Persist.write(key, value);
    }

    @Override
    public void clearCache() {
        Cache.clearCache();
    }

    @Override
    public void clearStorage() {
        Persist.clearStorage();
    }

    @Override
    public void run() {
        if (serverSocket != null) {
            serverRunning = true;
            logger.info("Now accepting client connections..");
            while (serverRunning) {
                try {
                    Socket client = serverSocket.accept();
                    ClientConnection connection = new ClientConnection(this, client);
                    Thread clientThread = new Thread(connection);
                    clientThread.start();
                    clientThreads.add(clientThread);

                    logger.info("Connected to " + client.getInetAddress().getHostName() + " on port " + client
                            .getPort());

                } catch (IOException e) {
                    logger.error("Error! Unable to establish connection. \r\n", e);
                }
            }
        }
        logger.info("Server stopped.");
    }

    @Override
    public void kill() {
        System.exit(-1);
    }

    @Override
    public void close() {
        serverRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + port, e);
        }
    }


    @Override
    public void start() {
        // TODO
    }

    @Override
    public void stop() {
        // TODO
    }

    @Override
    public void lockWrite() {
        // TODO
    }

    @Override
    public void unlockWrite() {
        // TODO
    }

    @Override
    public boolean moveData(String[] hashRange, String targetName) throws Exception {
        // TODO
        return false;
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 7) {
            throw new Exception("Incorrect server arguments!");
        }

        KVServer server = new KVServer(args[0], args[1], Integer.parseInt(args[2]));
        server.initKVServer(Integer.parseInt(args[3]), args[6], Integer.parseInt(args[4]), args[5]);
        Thread thread = new Thread(server);
        thread.start();

    }

}
