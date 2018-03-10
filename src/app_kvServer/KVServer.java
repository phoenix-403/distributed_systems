package app_kvServer;

import com.google.gson.Gson;
import common.helper.ZkConnector;
import common.helper.ZkNodeTransaction;
import common.messages.Metadata;
import common.messages.server_server.SrvSrvRequest;
import common.messages.server_server.SrvSrvResponse;
import common.messages.zk_server.ZkServerCommunication;
import common.messages.zk_server.ZkToServerRequest;
import common.messages.zk_server.ZkToServerResponse;
import ecs.ECSNode;
import ecs.ZkStructureNodes;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import static common.messages.Metadata.MAX_MD5;
import static common.messages.Metadata.MIN_MD5;
import static common.messages.server_server.SrvSrvCommunication.Request.TRANSFERE_DATA;
import static common.messages.server_server.SrvSrvCommunication.Response.TRANSFERE_FAIL;
import static common.messages.server_server.SrvSrvCommunication.Response.TRANSFERE_SUCCESS;
import static ecs.ZkStructureNodes.*;

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
    private boolean acceptingWriteRequests;

    private ZkConnector zkConnector;
    private ZooKeeper zooKeeper;
    private ZkNodeTransaction zkNodeTransaction;

    private int TIMEOUT = 20000;

    private Metadata metadata;

    private List<ClientConnection> clientConnections;

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
     * @param cacheSize specifies how many key-value pairs the server is allowed
     *                  to keep in-memory
     * @param strategy  specifies the cache replacement strategy in case the cache
     *                  is full and there is a GET- or PUT-request on a key that is
     *                  currently not contained in the cache. Options are "FIFO", "LRU",
     *                  and "LFU".
     */
    private void initKVServer(int port, int cacheSize, String strategy) throws Exception {

        try {
            new LogSetup("ds_data/" + name + "/logs/server.log", Level.ALL);
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
        if (!Persist.init("/" + name)) {
            logger.fatal("Can't start a server without a database!");
            // if persist is not available exit server.. cant live without persist but can live without cache
            System.exit(-1);
        }
        //setup the metaData
        updateMetadata(true);
        addMetadataWatch();
        addServerRequestWatch();

        serverRunning = false;
        acceptingRequests = false;
        acceptingWriteRequests = true; // don't worry overridden bt accepting req but write should be on by default
        // accept ecs commands
        addEcsCommandsWatch();

        clientConnections = new ArrayList<>();
        try {
            serverSocket = new ServerSocket(port);
            if (port == 0) {
                this.port = serverSocket.getLocalPort();
            }
            logger.info("Initialized! listening on port: " + serverSocket.getLocalPort());

            // adding hb node
            zkNodeTransaction.createZNode(ZkStructureNodes.HEART_BEAT.getValue() + "/" + name, null, CreateMode
                    .EPHEMERAL);

        } catch (IOException e) {
            logger.error("Error! Cannot open server socket: " + e.getMessage());
        } catch (InterruptedException | KeeperException e) {
            logger.error("Server " + name + " was not added to HB in zookeeper !!!");
        }
    }

    private void addEcsCommandsWatch() throws KeeperException, InterruptedException {
        zooKeeper.getChildren(ZkStructureNodes.ZK_SERVER_REQUEST.getValue(), event -> {
            if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                try {
                    List<String> requestIds =
                            zooKeeper.getChildren(ZkStructureNodes.ZK_SERVER_REQUEST.getValue(), false);
                    processRequest(requestIds);
                    addEcsCommandsWatch();
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });
    }


    private void processRequest(List<String> requestIds) throws Exception {
        Collections.sort(requestIds);

        String reqData;
        String requestId = requestIds.get(requestIds.size() - 1); // process latest request
        reqData = new String(zkNodeTransaction.read(ZkStructureNodes.ZK_SERVER_REQUEST.getValue() + "/" + requestId));
        ZkToServerRequest request = new Gson().fromJson(reqData, ZkToServerRequest.class);

        ZkServerCommunication.Response responseState;
        switch (request.getZkSvrRequest()) {
            case START:
                start();
                responseState = ZkServerCommunication.Response.START_SUCCESS;
                respond(request.getId(), responseState);
                break;
            case STOP:
                stop();
                responseState = ZkServerCommunication.Response.STOP_SUCCESS;
                respond(request.getId(), responseState);
                break;
            case SHUTDOWN:
                // for shutdown, i have to respond before closing the server
                responseState = ZkServerCommunication.Response.SHUTDOWN_SUCCESS;
                respond(request.getId(), responseState);
                Thread.sleep(2000);
                close();
                break;
            case REMOVE_NODES:
                boolean success;
                List<String> targetNode = request.getNodes();
                if (targetNode.contains(name)) {

                    ECSNode nextNode = metadata.getNextServer(name, targetNode);
                    if (nextNode != null) {
                        success = moveData(nextNode.getNodeHashRange(), nextNode.getNodeName());

                        if (success) {
                            responseState = ZkServerCommunication.Response.REMOVE_NODES_SUCCESS;
                            respond(request.getId(), responseState);
                            Thread.sleep(2000);
                            close();
                        } else {
                            responseState = ZkServerCommunication.Response.REMOVE_NODES_FAIL;
                            respond(request.getId(), responseState);
                        }
                    } else {
                        // if you come here it means u r removing yourself and no other nodes exist
                        // or you are removing all nodes!
                        logger.info("No servers to move data to");
                        responseState = ZkServerCommunication.Response.REMOVE_NODES_SUCCESS;
                        respond(request.getId(), responseState);
                        Thread.sleep(2000);
                        close();
                    }

                }
                break;
            default:
                // will never reach here unless enum is updated
                logger.error("Unknown ECS request!!");
        }
    }

    private void respond(int reqId, ZkServerCommunication.Response responseState) throws KeeperException,
            InterruptedException {
        ZkToServerResponse response = new ZkToServerResponse(reqId, name, responseState);
        zkNodeTransaction.createZNode(
                ZkStructureNodes.ZK_SERVER_RESPONSE.getValue() + ZkStructureNodes.RESPONSE.getValue(),
                new Gson().toJson(response, ZkToServerResponse.class).getBytes(), CreateMode
                        .EPHEMERAL_SEQUENTIAL);
    }

    private synchronized void addServerRequestWatch() throws KeeperException, InterruptedException {
        zooKeeper.getChildren(ZkStructureNodes.SERVER_SERVER_REQUEST.getValue(), event -> {
            if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                try {
                    List<String> reqNodes = zooKeeper.getChildren(ZkStructureNodes.SERVER_SERVER_REQUEST.getValue(),
                            false);
                    addServerRequestWatch();
                    handleServerRequest(reqNodes);
                } catch (KeeperException | InterruptedException e) {
                    logger.fatal("server request watch failed - !" + e.getMessage());
                    System.exit(-1);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });
    }

    private void handleServerRequest(List<String> reqNodes) throws Exception {
        // todo in all server-server... remove duplicates! //todo in get - check data that is being moved!
        String reqJson;
        for (String reqNode : reqNodes) {
            reqJson = new String(zkNodeTransaction.read(
                    ZkStructureNodes.SERVER_SERVER_REQUEST.getValue() + "/" + reqNode));
            Gson gson = new Gson();
            SrvSrvRequest req = gson.fromJson(reqJson, SrvSrvRequest.class);
            if (req.getTargetServer().equals(name)) {
                HashMap<String, String> newDataPairs = req.getKvToImport();
                Iterator it = newDataPairs.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry next = (Map.Entry) it.next();
                    if (!Persist.write((String) next.getKey(), (String) next.getValue())) {
                        logger.error("Write Not Successful!");
                        SrvSrvResponse response = new SrvSrvResponse(name, req.getServerName(), TRANSFERE_FAIL);
                        zkNodeTransaction.createZNode(SERVER_SERVER_RESPONSE.getValue() + RESPONSE.getValue(),
                                gson.toJson(response).getBytes(), CreateMode.EPHEMERAL_SEQUENTIAL);
                    }
                }
                logger.error("Write Successful!");
                SrvSrvResponse response = new SrvSrvResponse(name, req.getServerName(), TRANSFERE_SUCCESS);
                zkNodeTransaction.createZNode(SERVER_SERVER_RESPONSE.getValue() + RESPONSE.getValue(),
                        gson.toJson(response).getBytes(), CreateMode.EPHEMERAL_SEQUENTIAL);
            }
        }
    }

    private void addMetadataWatch() throws KeeperException, InterruptedException {
        zooKeeper.exists(ZkStructureNodes.METADATA.getValue(), event -> {
            if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                try {
                    updateMetadata(false);
                    addMetadataWatch();
                } catch (Exception e) {
                    logger.fatal("Metadata write failed!" + e.getMessage());
                    System.exit(-1);
                }
            }
        });
    }


    private void updateMetadata(boolean firstRun) throws Exception {
        String[] prevRange = null;

        if (!firstRun && metadata.getEcsNodes().size() > 0) {
            prevRange = new String[2];
            System.arraycopy(metadata.getRange(name), 0, prevRange, 0, 2);
        }

        String data = new String(zkNodeTransaction.read(ZkStructureNodes.METADATA.getValue()));
        metadata = new Gson().fromJson(data, Metadata.class);

        if (!firstRun && prevRange != null) {
            int prevRangeInt = prevRange[1].compareTo(prevRange[0]) < 0 ? prevRange[1].compareTo(prevRange[0]) :
                    prevRange[1].compareTo(prevRange[0]) + MAX_MD5.compareTo(MIN_MD5);
            int currRangeInt = metadata.getRange(name)[1].compareTo(metadata.getRange(name)[0]) < 0 ?
                    metadata.getRange(name)[1].compareTo(metadata.getRange(name)[0]) :
                    metadata.getRange(name)[1].compareTo(metadata.getRange(name)[0]) + MAX_MD5.compareTo(MIN_MD5);
            if (prevRangeInt > currRangeInt) {
                List<ECSNode> withinRangeNodes = metadata.getWithinRange(prevRange);
                for (ECSNode withinRangeNode : withinRangeNodes)
                    moveData(withinRangeNode.getNodeHashRange(), withinRangeNode.getNodeName());
            }
        }
    }

    public boolean isAcceptingRequests() {
        return acceptingRequests;
    }

    public boolean isAcceptingWriteRequests() {
        return acceptingWriteRequests;
    }

    public String getName() {
        return name;
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

    public Metadata getMetadata() {
        return this.metadata;
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
                    clientConnections.add(connection);
                    Thread clientThread = new Thread(connection);
                    clientThread.start();

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
        for (ClientConnection connection : clientConnections) {
            connection.close();
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + port, e);
        }
    }


    @Override
    public void start() {
        acceptingRequests = true;
    }

    @Override
    public void stop() {
        acceptingRequests = false;
    }

    @Override
    public void lockWrite() {
        acceptingWriteRequests = false;
    }

    @Override
    public void unlockWrite() {
        acceptingWriteRequests = true;
    }

    @Override
    public boolean moveData(String[] hashRange, String targetName) throws Exception {
        lockWrite();

        //handling wraparound case
        HashMap<String, String> myKeyValues = new HashMap<>();
        if (hashRange[0].compareTo(hashRange[1]) > 0) {
            myKeyValues.putAll(Persist.readRange(new String[]{hashRange[0], Metadata.MAX_MD5}));
            myKeyValues.putAll(Persist.readRange(new String[]{Metadata.MIN_MD5, hashRange[1]}));
        } else {
            myKeyValues.putAll(Persist.readRange(hashRange));
        }

        SrvSrvRequest request = new SrvSrvRequest(name, targetName, TRANSFERE_DATA, myKeyValues);
        zkNodeTransaction.createZNode(SERVER_SERVER_REQUEST.getValue() + REQUEST.getValue(),
                new Gson().toJson(request).getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
            List<String> respNodes = zooKeeper.getChildren(ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue(),
                    false);
            String respJSON;
            for (String respNode : respNodes) {
                respJSON = new String(zkNodeTransaction.read(
                        ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue() + "/" + respNode));
                System.out.println(respJSON);
                Gson gson = new Gson();
                SrvSrvResponse resp = gson.fromJson(respJSON, SrvSrvResponse.class);
                if (resp.getTargetServer().equals(name) && resp.getServerName().equals(request.getTargetServer())) {
                    if (hashRange[0].compareTo(hashRange[1]) > 0) {
                        Persist.deleteRange(new String[]{hashRange[0], Metadata.MAX_MD5});
                        Persist.deleteRange(new String[]{Metadata.MIN_MD5, hashRange[1]});
                    } else {
                        Persist.deleteRange(hashRange);
                    }
                    unlockWrite();
                    return resp.getResponse().equals(TRANSFERE_SUCCESS);
                }
            }
        }
        unlockWrite();
        return false;
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new Exception("Incorrect server arguments!");
        }

        KVServer server = new KVServer(args[0], args[1], Integer.parseInt(args[2]));
        server.initKVServer(Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
        Thread thread = new Thread(server);
        thread.start();

    }

}
