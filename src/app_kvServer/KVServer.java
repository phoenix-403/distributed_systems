package app_kvServer;

import com.google.gson.Gson;
import common.helper.ZkConnector;
import common.helper.ZkNodeTransaction;
import common.messages.Metadata;
import common.messages.server_server.SrvSrvCommunication;
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
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static app_kvServer.Persist.DELIMITER_PATTERN;
import static app_kvServer.Persist.dbFile;
import static common.messages.server_server.SrvSrvCommunication.Request.REPLICATE_DATA;
import static common.messages.server_server.SrvSrvCommunication.Request.TRANSFER_DATA;
import static common.messages.server_server.SrvSrvCommunication.Response.TRANSFERE_FAIL;
import static common.messages.server_server.SrvSrvCommunication.Response.TRANSFERE_FAIL_LOCK;
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

    private int TIMEOUT = 15000;

    private Metadata metadata;
    private String serverRange[] = null;
    private List<String[]> replicaRanges = new ArrayList<String[]>();

    private String EMPTY_SRV_SRV_REQ;
    private String EMPTY_SRV_SRV_RES;

    private List<ClientConnection> clientConnections;

    ScheduledExecutorService scheduler = null;
    private Future<?> replicationCancelButton = null;

    private ReentrantLock replicaDBLock = new ReentrantLock();

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
        logger.info(name + " me :) started!");

        // init req
        EMPTY_SRV_SRV_REQ = new Gson().toJson(new SrvSrvRequest("", "", null, null, null), SrvSrvRequest.class);
        EMPTY_SRV_SRV_RES = new Gson().toJson(new SrvSrvResponse("", "", null), SrvSrvResponse.class);
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
        // server-server req watch
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
            zkNodeTransaction.createZNode(ZkStructureNodes.NONE_HEART_BEAT.getValue() + "/" + name, null, CreateMode
                    .PERSISTENT);

        } catch (IOException e) {
            logger.error("Error! Cannot open server socket: " + e.getMessage());
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
                logger.info("Got start request");
                start();
                responseState = ZkServerCommunication.Response.START_SUCCESS;
                respond(request.getId(), responseState);

                scheduler = Executors.newSingleThreadScheduledExecutor();

                int initialDelay = 15;
                int periodicDelay = 20;

                replicationCancelButton = scheduler.scheduleAtFixedRate(() -> {
                            try {
                                for (String[] replicaRange : replicaRanges) {
                                    if (System.currentTimeMillis() - Long.parseLong(replicaRange[2]) > 40000) {
                                        logger.info("range has expired: " + replicaRange[0] + " | " + replicaRange[1]
                                                + " | " + replicaRange[2]);
                                        cleanOldReplicatedData(new String[]{replicaRange[0], replicaRange[1]});
                                    }
                                }
                                replicateData(null);
                            } catch (IOException | InterruptedException | KeeperException e) {
                                logger.error("Replicate Data Failed with Error: " + e.getMessage());
                            }
                        }, initialDelay, periodicDelay,
                        TimeUnit.SECONDS);
                break;
            case STOP:
                logger.info("Got stop request");
                stopScheduler();
                stop();
                responseState = ZkServerCommunication.Response.STOP_SUCCESS;
                respond(request.getId(), responseState);
                break;
            case TRANSFER_BACKUP_DATA:
                logger.info("Got transfer backup data request");

                if (moveReplicatedData(request.getCrashedServerHashRange(), request.getNodes().get(0))) {
                    logger.info("Moved backup data successfully");
                    responseState = ZkServerCommunication.Response.TRANSFER_BACKUP_DATA_SUCCESS;
                    respond(request.getId(), responseState);
                } else {
                    logger.error("backup data moving failed");
                    responseState = ZkServerCommunication.Response.TRANSFER_BACKUP_DATA_FAIL;
                    respond(request.getId(), responseState);
                }
                break;
            case SHUTDOWN:
                logger.info("Got shutdown request");
                // for shutdown, i have to respond before closing the server
                responseState = ZkServerCommunication.Response.SHUTDOWN_SUCCESS;
                respond(request.getId(), responseState);
                close();
                break;
            case REMOVE_NODES:
                logger.info("Received removeNode request");
                boolean success;
                List<String> targetNode = request.getNodes();
                if (targetNode.contains(name)) {
                    logger.info("remove req is for me!");

                    ECSNode nextNode = metadata.getNextServer(name, targetNode);

                    if (nextNode != null) {
                        logger.info("Attempting to move data to " + nextNode.getNodeName() + "!");
                        success = moveData(metadata.getRange(name), nextNode.getNodeName());

                        if (success) {
                            logger.info("Moved data successfully");
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
                        logger.info("Moved data successfully");
                        close();
                    }

                }
                break;
            default:
                // will never reach here unless enum is updated
                logger.error("Unknown ECS request!!");
        }
    }

    private void stopScheduler() {
        if (replicationCancelButton != null) {
            boolean cancelled = replicationCancelButton.cancel(true);
            if (cancelled) {
                logger.info("Current future canceled");
            }
        }
        if (scheduler != null) {
            scheduler.shutdown();
            logger.info("Scheduler Shutdown Completed");
        }
    }

    private void respond(int reqId, ZkServerCommunication.Response responseState) throws KeeperException,
            InterruptedException {
        ZkToServerResponse response = new ZkToServerResponse(reqId, name, responseState);
        zkNodeTransaction.createZNode(
                ZkStructureNodes.ZK_SERVER_RESPONSE.getValue() + ZkStructureNodes.RESPONSE.getValue(),
                new Gson().toJson(response, ZkToServerResponse.class).getBytes(), CreateMode
                        .PERSISTENT_SEQUENTIAL);
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

    private synchronized void handleServerRequest(List<String> reqNodes) throws Exception {
        String reqJson;
        for (String reqNode : reqNodes) {
            reqJson = new String(zkNodeTransaction.read(
                    ZkStructureNodes.SERVER_SERVER_REQUEST.getValue() + "/" + reqNode));

            Gson gson = new Gson();
            SrvSrvRequest req = gson.fromJson(reqJson, SrvSrvRequest.class);
            if (req.getTargetServer().equals(name)) {
                // ----------------------- nullifying req so it is not processed again
                // ------------------------------
                zkNodeTransaction.write(ZkStructureNodes.SERVER_SERVER_REQUEST.getValue() + "/" + reqNode,
                        EMPTY_SRV_SRV_REQ.getBytes());
                // --------------------------------------------------------------------------------------------------

                switch (req.getRequest()) {
                    case TRANSFER_DATA: {
                        logger.info("got a transfer data request - " + req.toString());
                        logger.info("lock-write");
                        lockWrite();

                        HashMap<String, String> newDataPairs = req.getKvToImport();
                        Iterator it = newDataPairs.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry next = (Map.Entry) it.next();
                            try {
                                if (!Persist.write((String) next.getKey(), (String) next.getValue())) {
                                    logger.error("Write Not Successful!");
                                    SrvSrvResponse response = new SrvSrvResponse(name, req.getServerName(),
                                            TRANSFERE_FAIL);
                                    zkNodeTransaction.createZNode(SERVER_SERVER_RESPONSE.getValue() + RESPONSE
                                                    .getValue(),
                                            gson.toJson(response).getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
                                }
                            } catch (IOException e) {
                                logger.error("Write Not Successful! " + e.getMessage());
                            } finally {
                                logger.info("Unlock write");
                                unlockWrite();
                            }
                        }

                        logger.info("Write Successful!");
                        SrvSrvResponse response = new SrvSrvResponse(name, req.getServerName(), TRANSFERE_SUCCESS);
                        zkNodeTransaction.createZNode(SERVER_SERVER_RESPONSE.getValue() + RESPONSE.getValue(),
                                gson.toJson(response).getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
                        logger.info("Responding with " + response.toString());
                        break;
                    }
                    case REPLICATE_DATA: {
                        try {
                            replicaDBLock.lock();
                            //locking to prevent data being written while a backup data transfer is happening

                            HashMap<String, String> newDataPairs = req.getKvToImport();
                            Iterator it = newDataPairs.entrySet().iterator();

                            //assuming no byzantine failures plox
                            if (!existsInReplica(req.getHashRange())) {
                                logger.info("Replica Records do not contain this new range!");
                                if (replicaRanges.size() < 2) {
                                    logger.info("Replica Records do not yet have 2");
                                } else if (replicaRanges.size() >= 2) {
                                    logger.info("Replica Records already has 2");
                                    cleanOldReplicatedData(req.getHashRange());
                                }
                                String[] insert = new String[]{req.getHashRange()[0], req.getHashRange()[1],
                                        Long.toString(System.currentTimeMillis())};
                                replicaRanges.add(insert);
                            }
                            if (req.getHashRange()[0].compareTo(req.getHashRange()[1]) >= 0) {
                                Persist.deleteRangeReplica(new String[]{req.getHashRange()[0], Metadata.MAX_MD5});
                                Persist.deleteRangeReplica(new String[]{Metadata.MIN_MD5, req.getHashRange()[1]});
                            } else {
                                Persist.deleteRangeReplica(req.getHashRange());
                            }
                            //plz

                            while (it.hasNext()) {
                                Map.Entry next = (Map.Entry) it.next();
                                if (!Persist.writeReplica((String) next.getKey(), (String) next.getValue())) {
                                    logger.error("Write Replica Not Successful!");
                                    SrvSrvResponse response = new SrvSrvResponse(name, req.getServerName(),
                                            TRANSFERE_FAIL);
                                    zkNodeTransaction.createZNode(SERVER_SERVER_RESPONSE.getValue() + RESPONSE
                                                    .getValue(),
                                            gson.toJson(response).getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
                                }
                            }

                            logger.info("Write Replica Successful!");
                            SrvSrvResponse response = new SrvSrvResponse(name, req.getServerName(), TRANSFERE_SUCCESS);
                            zkNodeTransaction.createZNode(SERVER_SERVER_RESPONSE.getValue() + RESPONSE.getValue(),
                                    gson.toJson(response).getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
                        } catch (Exception e) {
                            SrvSrvResponse response = new SrvSrvResponse(name, req.getServerName(),
                                    TRANSFERE_FAIL_LOCK);
                            zkNodeTransaction.createZNode(SERVER_SERVER_RESPONSE.getValue() + RESPONSE.getValue(),
                                    gson.toJson(response).getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
                        } finally {
                            replicaDBLock.unlock();
                        }
                        break;
                    }
                }

            }
        }

    }

    private void cleanOldReplicatedData(String[] newRange) throws IOException {
        for (String[] replicaRange : replicaRanges) {
            // wrap-around case
            if (replicaRange[0].compareTo(replicaRange[1]) >= 0) {
                String[] endInterval = new String[2];
                endInterval[0] = Metadata.MIN_MD5;
                endInterval[1] = replicaRange[1];
                String[] startInterval = new String[2];
                startInterval[0] = replicaRange[0];
                startInterval[1] = Metadata.MAX_MD5;
                if (metadata.isWithinRange(newRange[0], startInterval)
                        || metadata.isWithinRange(newRange[1], startInterval)
                        || metadata.isWithinRange(newRange[0], endInterval)
                        || metadata.isWithinRange(newRange[1], endInterval)) {
                    Persist.deleteRangeReplica(startInterval);
                    Persist.deleteRangeReplica(endInterval);
                    replicaRanges.remove(replicaRange);
                }
            } else if (metadata.isWithinRange(newRange[0], replicaRange)
                    || metadata.isWithinRange(newRange[1], replicaRange)) {
                Persist.deleteRangeReplica(replicaRange);
                replicaRanges.remove(replicaRange);
            }
        }
    }

    private boolean existsInReplica(String[] hashRange) {
        boolean exists = false;
        for (String[] replicaRange : replicaRanges) {
            logger.info("Comparing replica ranges: " + hashRange[0]  + "-" + " | " + replicaRange);
            if (hashRange[0].equals(replicaRange[0]) && hashRange[1].equals(replicaRange[1])) {
                exists = true;
                replicaRange[2] = Long.toString(System.currentTimeMillis());
            }
        }
        return exists;
    }

    private void addMetadataWatch() throws KeeperException, InterruptedException {
        zooKeeper.exists(ZkStructureNodes.METADATA.getValue(), event -> {
            if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                try {
                    addMetadataWatch();
                    logger.info("Updating metadata!");
                    updateMetadata(false);
                } catch (Exception e) {
                    logger.fatal("Metadata write failed!" + e.getMessage());
                }
            }
        });
    }

    // will move_data from server if the server is affected!!!!!
    private synchronized void updateMetadata(boolean firstRun) throws KeeperException, InterruptedException,
            IOException {
        // if first run is true .. it loads AN EMPTY metadata
        String data = new String(zkNodeTransaction.read(ZkStructureNodes.METADATA.getValue()));
        metadata = new Gson().fromJson(data, Metadata.class);
        logger.info("updating metadata to: " + data);

        if (!firstRun) {
            String[] newRange = metadata.getRange(name);

            if (serverRange != null) {
                logger.info("setting up to prepare to move data if needed");
                //third run
                List<ECSNode> updatedNodes = new ArrayList<>(metadata.getEcsNodes());
                // removing myself and splitting wrap around server to 2 pieces
                ECSNode updatedNode;
                List<ECSNode> wrapAroundSplitNode = new ArrayList<>();
                Iterator<ECSNode> updatedNodesIterator = updatedNodes.iterator();
                while (updatedNodesIterator.hasNext()) {
                    updatedNode = updatedNodesIterator.next();
                    if (updatedNode.getNodeName().equals(name)) {
                        updatedNodesIterator.remove();
                    } else if (updatedNode.getNodeHashRange()[0].compareTo(updatedNode.getNodeHashRange()[1]) > 0) {
                        wrapAroundSplitNode.add(new ECSNode(updatedNode.getNodeName(), updatedNode.getNodeHost(),
                                updatedNode.getNodePort(), new String[]{updatedNode.getNodeHashRange()[0], Metadata
                                .MAX_MD5}, updatedNode.isReserved()));
                        wrapAroundSplitNode.add(new ECSNode(updatedNode.getNodeName(), updatedNode.getNodeHost(),
                                updatedNode.getNodePort(), new String[]{Metadata.MIN_MD5, updatedNode.getNodeHashRange
                                ()[1]}, updatedNode.isReserved()));
                        updatedNodesIterator.remove();
                    }
                }
                updatedNodes.addAll(wrapAroundSplitNode);

                // calculating the three scenarios

                if (serverRange[0].compareTo(serverRange[1]) > 0) {
                    calculate(new String[]{serverRange[0], Metadata.MAX_MD5}, updatedNodes);
                    calculate(new String[]{Metadata.MIN_MD5, serverRange[1]}, updatedNodes);
                } else {
                    calculate(serverRange, updatedNodes);
                }


                //updating range to actual current value
                this.serverRange = new String[]{newRange[0], newRange[1]};
            } else {
                // second run which is when it receives its official metadata for the first time
                // therefore no moving data from this server
                serverRange = new String[]{newRange[0], newRange[1]};

                //attempting to read backup data
                logger.info("attempting to load backup data");
                try {
                    List<String> backupNodes = zooKeeper.getChildren(ZkStructureNodes.BACKUP_DATA.getValue(), false);
                    String path, key, value, kv;
                    for (String backupNode : backupNodes) {
                        path = ZkStructureNodes.BACKUP_DATA.getValue() + "/" + backupNode;
                        kv = new String(zkNodeTransaction.read(path));
                        key = kv.split(DELIMITER_PATTERN)[0];
                        value = kv.split(DELIMITER_PATTERN)[1];
                        if (metadata.isWithinRange(key, this.getName())) {
                            Persist.write(key, value);
                            zkNodeTransaction.delete(path);
                        }
                    }
                } catch (KeeperException | InterruptedException e) {
                    logger.error("failed to import key from backup!");
                }
            }
        }
    }

    private void calculate(String[] oldRange, List<ECSNode> nodesUpdatedHashRange) throws InterruptedException,
            IOException, KeeperException {

        for (ECSNode updatedNode : nodesUpdatedHashRange) {
            //scenario 1 -- all of new node lies in this server range
            if (oldRange[0].compareTo(updatedNode.getNodeHashRange()[0]) <= 0
                    && oldRange[1].compareTo(updatedNode.getNodeHashRange()[0]) >= 0) {
                moveData(updatedNode.getNodeHashRange(), updatedNode.getNodeName());
                clearCache();
            }//scenario 2 -- only some of the first ranges in this server
            else if (oldRange[0].compareTo(updatedNode.getNodeHashRange()[1]) < 0
                    && oldRange[1].compareTo(updatedNode.getNodeHashRange()[1]) >= 0) {
                moveData(new String[]{oldRange[0], updatedNode.getNodeHashRange()[1]}, updatedNode.getNodeName());
                clearCache();
            }//scenario 3 -- only some of the last ranges in this server
            else if (oldRange[0].compareTo(updatedNode.getNodeHashRange()[0]) <= 0
                    && oldRange[1].compareTo(updatedNode.getNodeHashRange()[0]) > 0) {
                moveData(new String[]{updatedNode.getNodeHashRange()[0], oldRange[1]}, updatedNode.getNodeName());
                clearCache();
            }
        }
        replicateData(oldRange);
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

    public List<String[]> getReplicaRanges() {
        return replicaRanges;
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
                    logger.error("Error! Unable to establish connection.\n", e);
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
        logger.info("Entering close()");
        try {
            zkNodeTransaction.delete(ZkStructureNodes.NONE_HEART_BEAT.getValue() + "/" + name);
            zkNodeTransaction.delete(ZkStructureNodes.HEART_BEAT.getValue() + "/" + name);
        } catch (KeeperException | InterruptedException e) {
            logger.fatal("Unable to delete HR Node");
        }

        stopScheduler();

        // backing up data onto zookeeper
        try {
            ArrayList<String> fileLines = (ArrayList<String>) Files.readAllLines(dbFile.toPath());
            logger.info("Adding keys to backup: " + fileLines.toString());
            for (String fileLine : fileLines) {
                zkNodeTransaction.createZNode(ZkStructureNodes.BACKUP_DATA.getValue()
                        + ZkStructureNodes.NODE.getValue(), fileLine.getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
            }
        } catch (IOException | InterruptedException | KeeperException e) {
            logger.error("failed to create a copy of the data on zookeeper b4 shutdown!");
        }

        logger.info("Closing zookeeper");
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            logger.error("Unable to close zookeeper");
            zooKeeper = null;
        }

        logger.info("Closing client connections");
        serverRunning = false;
        for (ClientConnection connection : clientConnections) {
            connection.close();
        }

        logger.info("Closing server socket");
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + port, e);
        }

        System.exit(0);
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

    private synchronized void cleanseOldResponses() throws KeeperException, InterruptedException {
        // ----------------------- Cleanse the old responses
        // ------------------------------
        List<String> respNodes = zooKeeper.getChildren(ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue(),
                false);
        for (String respNode : respNodes) {
            SrvSrvResponse resp = new Gson().fromJson(new String(zkNodeTransaction.read(
                    ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue() + "/" + respNode)), SrvSrvResponse.class);
            if (resp.getTargetServer().equals(name)) {
                zkNodeTransaction.write(ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue() + "/" + respNode,
                        EMPTY_SRV_SRV_RES.getBytes());
            }
        }
        // --------------------------------------------------------------------------------------------------
    }

    private synchronized void replicateData(String[] hashRange) throws IOException, KeeperException,
            InterruptedException {
        if (hashRange == null)
            hashRange = serverRange;
        int i = 0;

        //handling wraparound case
        HashMap<String, String> myKeyValues = getKeyValues(serverRange);

        ECSNode nextNode = metadata.getNextServer(name);
        cleanseOldResponses();
        while (nextNode != null && !nextNode.getNodeName().equals(name) && i < 2) {
            final String nextName = nextNode.getNodeName();
            final String[] finalRange = hashRange;
            logger.info("Replicating to " + nextName + " on iteration " + i);
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    sendServerReq(nextName, myKeyValues, finalRange,
                            REPLICATE_DATA);
                } catch (Exception e) {
                    logger.error("Replicate Data Failed with Error: " + e.getMessage());
                }
            });
            i++;
            nextNode = metadata.getNextServer(nextNode.getNodeName());
        }
    }

    private HashMap<String, String> getKeyValues(String[] hashRange) throws IOException {
        //handling wraparound case
        HashMap<String, String> myKeyValues = new HashMap<>();
        if (hashRange[0].compareTo(hashRange[1]) > 0) {
            myKeyValues.putAll(Persist.readRange(new String[]{hashRange[0], Metadata.MAX_MD5}));
            myKeyValues.putAll(Persist.readRange(new String[]{Metadata.MIN_MD5, hashRange[1]}));
        } else {
            myKeyValues.putAll(Persist.readRange(hashRange));
        }
        return myKeyValues;
    }

    private HashMap<String, String> getReplicatedKeyValues(String[] hashRange) throws IOException {
        //handling wraparound case
        HashMap<String, String> myKeyValues = new HashMap<>();
        if (hashRange[0].compareTo(hashRange[1]) > 0) {
            myKeyValues.putAll(Persist.readRangeReplica(new String[]{hashRange[0], Metadata.MAX_MD5}));
            myKeyValues.putAll(Persist.readRangeReplica(new String[]{Metadata.MIN_MD5, hashRange[1]}));
        } else {
            myKeyValues.putAll(Persist.readRangeReplica(hashRange));
        }
        return myKeyValues;
    }

    private boolean moveReplicatedData(String[] hashRange, String targetName) {
        try {
            logger.info("locking replicated db");
            replicaDBLock.lock();
            cleanseOldResponses();
            HashMap<String, String> myKeyValues = getReplicatedKeyValues(hashRange);

            if (sendServerReq(targetName, myKeyValues, hashRange, TRANSFER_DATA)) {
                logger.info("got a srv-srv response for replicated data");

                logger.info("unlock write and return success");
                return true;
            } else
                logger.info("unlock write and return fail");
        } catch (Exception e) {
            logger.error("Move Replicated Data Failed with Error: " + e.getMessage());
        } finally {
            logger.info("unlocking replicated db");
            replicaDBLock.unlock();
        }

        return false;
    }

    @Override
    public boolean moveData(String[] hashRange, String targetName) throws IOException, KeeperException,
            InterruptedException {
        logger.info("locking server!");
        lockWrite();

        //handling wraparound case
        HashMap<String, String> myKeyValues = getKeyValues(hashRange);

        cleanseOldResponses();

        if (sendServerReq(targetName, myKeyValues, hashRange, TRANSFER_DATA)) {
            logger.info("got a srv-srv response for move data");

            if (hashRange[0].compareTo(hashRange[1]) > 0) {
                Persist.deleteRange(new String[]{hashRange[0], Metadata.MAX_MD5});
                Persist.deleteRange(new String[]{Metadata.MIN_MD5, hashRange[1]});
            } else {
                Persist.deleteRange(hashRange);
            }
            logger.info("unlock write and return success");
            unlockWrite();
            return true;
        } else
            logger.info("unlock write and return fail");
        unlockWrite();
        return false;
    }

    private boolean sendServerReq(String targetName, HashMap<String, String> myKeyValues, String[] hashRange,
                                  SrvSrvCommunication.Request requestType) throws KeeperException,
            InterruptedException {

        logger.info("creating a server-server req");
        SrvSrvRequest request = new SrvSrvRequest(name, targetName, hashRange, requestType, myKeyValues);
        zkNodeTransaction.createZNode(SERVER_SERVER_REQUEST.getValue() + REQUEST.getValue(),
                new Gson().toJson(request).getBytes(), CreateMode.PERSISTENT_SEQUENTIAL);
        logger.info("requesting with: " + request.toString());

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
            List<String> respNodes = zooKeeper.getChildren(ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue(),
                    false);

            for (String respNode : respNodes) {
                String respJSON = new String(zkNodeTransaction.read(
                        ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue() + "/" + respNode));
                Gson gson = new Gson();
                SrvSrvResponse resp = gson.fromJson(respJSON, SrvSrvResponse.class);
                if (resp.getTargetServer().equals(name) && resp.getServerName().equals(request.getTargetServer())) {
                    // ----------------------- nullifying res so it is not processed again
                    // ------------------------------
                    zkNodeTransaction.write(ZkStructureNodes.SERVER_SERVER_RESPONSE.getValue() + "/" + respNode,
                            EMPTY_SRV_SRV_RES.getBytes());
                    // --------------------------------------------------------------------------------------------------
                    logger.info("server responded with" + resp.toString());
                    return resp.getResponse().equals(TRANSFERE_SUCCESS);
                }
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        //        if (args.length != 6) {
        //            throw new Exception("Incorrect server arguments!");
        //        }

        KVServer server = new KVServer(args[0], args[1], Integer.parseInt(args[2]));
        server.initKVServer(Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
        Thread thread = new Thread(server);
        thread.start();

    }

}
