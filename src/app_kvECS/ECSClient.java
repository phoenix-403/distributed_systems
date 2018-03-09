package app_kvECS;

import com.google.gson.Gson;
import common.helper.ConsistentHash;
import common.helper.Script;
import common.helper.ZkConnector;
import common.helper.ZkNodeTransaction;
import common.messages.Metadata;
import common.messages.ZkServerCommunication;
import common.messages.ZkToServerRequest;
import common.messages.ZkToServerResponse;
import ecs.ECSNode;
import ecs.IECSNode;
import ecs.ZkStructureNodes;
import logger.LogSetup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.ClientCnxn;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static app_kvECS.ECSClient.ArgType.INTEGER;
import static app_kvECS.ECSClient.ArgType.STRING;
import static common.helper.Script.createBashScript;
import static common.helper.Script.runScript;

public class ECSClient implements IECSClient {

    private static Logger logger = LogManager.getLogger(ECSClient.class);
    private static Logger loggerCnxn = LogManager.getLogger(ClientCnxn.class);

    private static final String PROMPT = "ECS_Client> ";
    private boolean stopClient = false;

    private String zkAddress;
    private int zkPort;

    private ZooKeeper zooKeeper;
    private ZkConnector zkConnector;
    private ZkNodeTransaction zkNodeTransaction;

    //zookeeper communication timeout
    private int reqResId = 0;
    private static final int TIME_OUT = 10000;

    private List<ECSNode> ecsNodes = new ArrayList<>();
    private Metadata metadata;

    enum ArgType {
        INTEGER,
        STRING,
    }

    public ECSClient(String configFile) throws IOException, EcsException {

        // setting up log
        try {
            new LogSetup("logs/ecs/ecs_client.log", Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // setting zookeeper variables
        zkAddress = "localhost";
        zkPort = 2181;

        //disable ClientCnxn messages
        loggerCnxn.setLevel(Level.OFF);

        // reading config file
        configureAvailableNodes(configFile);
    }

    private void configureAvailableNodes(String configFile) throws EcsException, IOException {
        File file = new File("src/app_kvECS/" + configFile);
        if (!file.exists()) {
            throw new EcsException("Config file does not exist!");
        }

        final String DELIMITER = " ";
        final String DELIMITER_PATTERN = Pattern.quote(DELIMITER);

        ArrayList<String> fileLines = (ArrayList<String>) Files.readAllLines(file.toPath());
        for (String line : fileLines) {
            String[] tokenizedLine = line.split(DELIMITER_PATTERN);
            ecsNodes.add(new ECSNode(tokenizedLine[0], tokenizedLine[1], Integer.parseInt(tokenizedLine[2]), null,
                    false));
        }
        System.out.println(ecsNodes);

    }

    private void startZK() throws InterruptedException, IOException, KeeperException {
        // starting zookeeper on local machine on the default port and waiting for script to finish
        String zkStartScript = System.getProperty("user.dir") + "/src/app_kvECS/startZK.sh";
        Process startZkProcess = runScript(zkStartScript, logger);
        startZkProcess.waitFor();

        // connecting to zookeeper
        zkConnector = new ZkConnector();
        zooKeeper = zkConnector.connect(zkAddress + ":" + zkPort);

        // setting up
        setupZk();

    }

    private void setupZk() throws KeeperException, InterruptedException {
        // making sure zookeeper is clean (haven't run before)
        zkNodeTransaction = new ZkNodeTransaction(zooKeeper);
        zkNodeTransaction.delete(ZkStructureNodes.ROOT.getValue());

        // setting up structural nodes
        zkNodeTransaction.createZNode(ZkStructureNodes.HEART_BEAT.getValue(), null, CreateMode.PERSISTENT);
        zkNodeTransaction.createZNode(ZkStructureNodes.METADATA.getValue(), null, CreateMode.PERSISTENT);
        zkNodeTransaction.createZNode(ZkStructureNodes.ZK_SERVER_REQUESTS.getValue(), null, CreateMode.PERSISTENT);
        zkNodeTransaction.createZNode(ZkStructureNodes.ZK_SERVER_RESPONSE.getValue(), null, CreateMode.PERSISTENT);
        zkNodeTransaction.createZNode(ZkStructureNodes.SERVER_SERVER_COMMANDS.getValue(), null, CreateMode.PERSISTENT);

        // writing an empty metadata
        metadata = new Metadata(new ArrayList<>());
        zkNodeTransaction.write(ZkStructureNodes.METADATA.getValue(),
                new Gson().toJson(metadata, Metadata.class).getBytes());
    }

    private void stopZK() throws InterruptedException {
        zkConnector.close();
        String zkStopScript = System.getProperty("user.dir") + "/src/app_kvECS/stopZK.sh";
        runScript(zkStopScript, logger);
    }


    @Override
    public boolean start() throws KeeperException, InterruptedException, EcsException {
        int noActiveServers = getNodesWithStatus(true).size();

        int reqId = reqResId++;
        ZkToServerRequest request = new ZkToServerRequest(reqId, ZkServerCommunication.Request.START);
        List<ZkToServerResponse> responses = processReqResp(request);

        if (noActiveServers == responses.size()) {
            for (ZkToServerResponse response : responses) {
                if (!response.getZkSvrResponse().equals(ZkServerCommunication.Response.START_SUCCESS)) {
                    throw new EcsException("An unexpected response to start command!!");
                }
            }
            return true;
        } else {
            StringBuilder failedServers = new StringBuilder();

            ArrayList<IECSNode> runningNodes = getNodesWithStatus(true);

            boolean serverResponded;

            for (IECSNode iecsNode : runningNodes) {
                serverResponded = false;
                for (ZkToServerResponse response : responses) {
                    if (iecsNode.getNodeName().equals(response.getServerName())) {
                        serverResponded = true;
                        break;
                    }
                }

                if (!serverResponded) {
                    failedServers.append(iecsNode.getNodeName()).append(" ");
                }

            }

            logger.error(failedServers + "did not start or timed out!");
            return false;
        }

    }

    @Override
    public boolean stop() throws KeeperException, InterruptedException, EcsException {
        int noActiveServers = getNodesWithStatus(true).size();

        int reqId = reqResId++;
        ZkToServerRequest request = new ZkToServerRequest(reqId, ZkServerCommunication.Request.STOP);
        List<ZkToServerResponse> responses = processReqResp(request);

        if (noActiveServers == responses.size()) {
            for (ZkToServerResponse response : responses) {
                if (!response.getZkSvrResponse().equals(ZkServerCommunication.Response.STOP_SUCCESS)) {
                    throw new EcsException("An unexpected response to stop command!!");
                }
            }
            return true;
        } else {
            StringBuilder failedServers = new StringBuilder();

            ArrayList<IECSNode> runningNodes = getNodesWithStatus(true);

            boolean serverResponded;

            for (IECSNode iecsNode : runningNodes) {
                serverResponded = false;
                for (ZkToServerResponse response : responses) {
                    if (iecsNode.getNodeName().equals(response.getServerName())) {
                        serverResponded = true;
                        break;
                    }
                }

                if (!serverResponded) {
                    failedServers.append(iecsNode.getNodeName()).append(" ");
                }

            }

            logger.error(failedServers + "did not stop or timed out!");
            return false;
        }


    }

    @Override
    public boolean shutdown() throws KeeperException, InterruptedException, EcsException {
        int noActiveServers = getNodesWithStatus(true).size();

        int reqId = reqResId++;
        ZkToServerRequest request = new ZkToServerRequest(reqId, ZkServerCommunication.Request.SHUTDOWN);
        List<ZkToServerResponse> responses = processReqResp(request);

        if (noActiveServers == responses.size()) {
            for (ZkToServerResponse response : responses) {
                if (!response.getZkSvrResponse().equals(ZkServerCommunication.Response.SHUTDOWN_SUCCESS)) {
                    throw new EcsException("An unexpected response to shutdown command!!");
                }
            }
            return true;
        } else {
            List<String> activeServers =
                    zooKeeper.getChildren(ZkStructureNodes.HEART_BEAT.getValue(), false);

            if (activeServers.size() == 0) {
                return true;
            }
            logger.error(activeServers + "did not shutdown!");
            return false;
        }


    }

    private List<ZkToServerResponse> processReqResp(ZkToServerRequest request) throws KeeperException,
            InterruptedException {
        int noActiveServers = getNodesWithStatus(true).size();

        // Sending request via zookeeper
        int reqId = request.getId();
        zkNodeTransaction.createZNode(ZkStructureNodes.ZK_SERVER_REQUESTS.getValue() + ZkStructureNodes.REQUEST
                .getValue(), new Gson().toJson(request, ZkToServerRequest.class).getBytes(), CreateMode
                .EPHEMERAL_SEQUENTIAL);

        List<ZkToServerResponse> responses = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TIME_OUT && responses.size() != noActiveServers) {
            // receiving response
            findResponseId(reqId, responses);
        }

        return responses;
    }

    private void findResponseId(int reqId, List<ZkToServerResponse> responses) throws
            KeeperException, InterruptedException {

        ZkToServerResponse response;
        List<String> respNodePaths = zooKeeper.getChildren(ZkStructureNodes.ZK_SERVER_RESPONSE.getValue(), false);
        for (String nodePath : respNodePaths) {
            response = new Gson().fromJson(
                    new String(zkNodeTransaction.read(ZkStructureNodes.ZK_SERVER_RESPONSE.getValue() + "/" + nodePath)),
                    ZkToServerResponse.class);
            if (response != null && response.getId() == reqId) {
                responses.add(response);
                zkNodeTransaction.delete(ZkStructureNodes.ZK_SERVER_RESPONSE.getValue() + "/" + nodePath);
            }
        }
    }

    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        ArrayList<IECSNode> list = (ArrayList<IECSNode>) addNodes(1, cacheStrategy, cacheSize);

        if (list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        if (count == 0) {
            return new ArrayList<>();
        }

        ArrayList<IECSNode> newEcsNodes = (ArrayList<IECSNode>) setupNodes(count, cacheStrategy, cacheSize);

        // Launch the server processes
        createRunSshScript(newEcsNodes, cacheStrategy, cacheSize);

        // call await nodes to wait for processes to start
        boolean success = awaitNodes(count, TIME_OUT);

        if (success) {
            // write node to added by setting boolean in ecs node
            for (IECSNode newEcsNode : newEcsNodes) {
                ecsNodes.get(ecsNodes.indexOf(newEcsNode)).setReserved(true);
                ((ECSNode) newEcsNode).setReserved(true);
            }
            logger.info("All nodes were added");
        } else {
            List<String> failedServers = new ArrayList<>();
            try {
                List<String> activeServers = zooKeeper.getChildren(ZkStructureNodes.HEART_BEAT.getValue(), false);

                Iterator<IECSNode> newEcsNodeIterator = newEcsNodes.iterator();
                while (newEcsNodeIterator.hasNext()) {
                    IECSNode newEcsNode = newEcsNodeIterator.next();
                    if (activeServers.contains(newEcsNode.getNodeName())) {
                        ecsNodes.get(ecsNodes.indexOf(newEcsNode)).setReserved(true);
                        ((ECSNode) newEcsNode).setReserved(true);
                    } else {
                        failedServers.add(newEcsNode.getNodeName());
                        newEcsNodeIterator.remove();
                    }
                }

                if (failedServers.size() > 0) {
                    logger.error("Timeout reached. Servers: " + failedServers + " might not has/have added");
                }

            } catch (KeeperException | InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

        // did not start with metadata to account for failures so doing them at the end
        ConsistentHash consistentHash = new ConsistentHash(ecsNodes);
        consistentHash.hash();

        // updating data
        for (IECSNode newEcsNode : newEcsNodes) {
            for (ECSNode ecsNode : ecsNodes) {
                if (newEcsNode.getNodeName().equals(ecsNode.getNodeName())) {
                    ((ECSNode) newEcsNode).setNodeHashRange(ecsNode.getNodeHashRange());
                }
            }
        }

        metadata = new Metadata(ecsNodes);
        try {
            zkNodeTransaction.write(ZkStructureNodes.METADATA.getValue(), new Gson().toJson(metadata, Metadata.class)
                    .getBytes());
        } catch (KeeperException | InterruptedException e) {
            logger.error("Metadata was not updated! " + e.getMessage());
        }

        return newEcsNodes;
    }

    private void createRunSshScript(ArrayList<IECSNode> iEcsNodes, String cacheStrategy, int cacheSize) {

        ECSNode ecsNode;
        StringBuilder scriptContent = new StringBuilder();

        for (IECSNode iEcsNode : iEcsNodes) {
            ecsNode = (ECSNode) iEcsNode;
            scriptContent.append("ssh -n ").append(ecsNode.getNodeHost()).append(" ").append("nohup java -jar ")
                    .append("~/IdeaProjects/distributed_systems/m2-server.jar ").append(ecsNode.getNodeName()).append
                    (" ")
                    .append
                            (zkAddress).append(" ").append(zkPort).append(" ").append(ecsNode.getNodePort()).append("" +
                    " ").append
                    (cacheSize).append(" ").append(cacheStrategy).append(" ").append("&\n");
        }

        String scriptPath = System.getProperty("user.dir") + "/src/app_kvECS/ssh.sh";

        createBashScript(scriptPath, scriptContent.toString(), logger);
        Script.runScript(scriptPath, logger);

    }

    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        List<IECSNode> nodesToSetup = getNodesWithStatus(false);
        int size = nodesToSetup.size();
        if (count > size) {
            logger.error("Trying to setup " + count + " nodes but only " + size + " available!");
            return null;
        }

        // shuffling and removing random objects to only add required nodes
        Collections.shuffle(nodesToSetup);
        for (int i = 0; i < size - count; i++) {
            nodesToSetup.remove(0);
        }

        return nodesToSetup;
    }

    private ArrayList<IECSNode> getNodesWithStatus(boolean reserved) {
        List<IECSNode> availableNodes = new ArrayList<>(ecsNodes);
        CollectionUtils.filter(availableNodes, ecsNode -> ((ECSNode) ecsNode).isReserved() == reserved);
        return (ArrayList<IECSNode>) availableNodes;
    }


    @Override
    public boolean awaitNodes(int count, int timeout) {
        List<ECSNode> runningNodes = new ArrayList<>(ecsNodes);
        CollectionUtils.filter(runningNodes, ECSNode::isReserved);
        int preexistingHrNodes = runningNodes.size();

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime <= timeout) {
            try {
                int numberHrChildrenNodes = zooKeeper.getChildren(ZkStructureNodes.HEART_BEAT.getValue(), false).size();
                if (numberHrChildrenNodes - preexistingHrNodes == count) {
                    return true;
                }
            } catch (KeeperException | InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

        return false;
    }

    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        // TODO
        return false;
    }

    @Override
    public Map<String, IECSNode> getNodes() {
        Map<String, IECSNode> map = new HashMap<>();

        for (ECSNode ecsNode : ecsNodes) {
            if (ecsNode.isReserved()) {
                map.put(ecsNode.getNodeName(), ecsNode);
            }
        }

        return map;
    }

    @Override
    public IECSNode getNodeByKey(String key) {
        try {
            Metadata metadata = new Gson().fromJson(
                    new String(zkNodeTransaction.read(ZkStructureNodes.METADATA.getValue())),
                    Metadata.class);

            return metadata.getResponsibleServer(key);

        } catch (KeeperException | InterruptedException e) {
            logger.error(e.getMessage());
        }
        return null;
    }


    /**
     * @param args config file
     */
    public static void main(String[] args) throws EcsException, IOException, InterruptedException, KeeperException {
        if (args.length != 1) {
            throw new EcsException("Incorrect # of arguments for ECS Client!");
        }

        ECSClient app = new ECSClient(args[0]);
        app.startZK();
        app.run();
        app.stopZK();
    }

    private void run() throws EcsException, KeeperException, InterruptedException {
        while (!stopClient) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);

            try {
                String cmdLine = stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                stopClient = true;
                printError("CLI does not respond - Application terminated ");
            }
        }
    }

    private void handleCommand(String cmdLine) throws EcsException, KeeperException, InterruptedException {
        String[] tokens = cmdLine.split("\\s+");
        Arrays.stream(tokens)
                .filter(s -> s != null && s.length() > 0
                ).collect(Collectors.toList()).toArray(tokens);

        if (tokens.length != 0 && tokens[0] != null) {
            switch (tokens[0]) {
                case "start": {
                    System.out.println(PROMPT + start());
                    break;
                }
                case "stop": {
                    System.out.println(PROMPT + stop());
                    break;
                }
                case "shutdown": {
                    System.out.println(PROMPT + shutdown());
                    break;
                }
                case "addNode": {
                    Object[] a = getArguments(tokens, new ArgType[]{STRING, INTEGER});
                    if (a == null)
                        return;
                    IECSNode node = addNode((String) a[0], (int) a[1]);
                    if (node == null) {
                        System.out.println(PROMPT + "No nodes added!");
                    } else {
                        System.out.println(PROMPT + node.getNodeName() + " started!");
                    }
                    break;
                }
                case "addNodes": {
                    Object[] a = getArguments(tokens, new ArgType[]{INTEGER, STRING, INTEGER});
                    if (a == null)
                        return;
                    Collection<IECSNode> nodes = addNodes((int) a[0], (String) a[1], (int) a[2]);

                    if (nodes.isEmpty()) {
                        System.out.println(PROMPT + "No nodes added!");
                    } else {
                        for (IECSNode node : nodes) {
                            System.out.println(node);
                        }
                        System.out.println("was/were added!");
                    }
                    break;
                }
//                case "setupNodes": {
//                    Object[] a = getArguments(tokens, new ArgType[]{INTEGER, STRING, INTEGER});
//                    if (a == null)
//                        return;
//                    Collection<IECSNode> nodes = setupNodes((int) a[0], (String) a[1], (int) a[2]);
//                    break;
//                }
//                case "awaitNodes": {
//                    Object[] a = getArguments(tokens, new ArgType[]{INTEGER, INTEGER});
//                    if (a == null)
//                        return;
//                    try {
//                        awaitNodes((int) a[0], (int) a[1]);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                }
                case "removeNodes": {
                    if (tokens.length < 2) {
                        printHelp();
                        return;
                    }
                    ArrayList<String> temp = new ArrayList<String>(
                            Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length)));
                    removeNodes(temp);
                    break;
                }
                case "getNodes": {
                    Map<String, IECSNode> nodes = getNodes();
                    break;
                }
                case "getNodeByKey": {
                    Object[] a = getArguments(tokens, new ArgType[]{STRING});
                    if (a == null)
                        return;
                    IECSNode node = getNodeByKey((String) a[0]);
                    break;
                }
                case "logLevel": {
                    if (tokens.length == 2) {
                        String level = setLevel(tokens[1]);
                        if (StringUtils.isEmpty(level)) {
                            printError("No valid log level!");
                            printPossibleLogLevels();
                        } else {
                            System.out.println(PROMPT +
                                    "Log level changed to level " + level);
                        }
                    } else {
                        printError("Invalid number of parameters!");
                    }

                    break;
                }
                case "help": {
                    printHelp();
                    break;
                }
                case "quit":
                    stopClient = true;
                    break;
                default: {
                    printError("Unknown command");
                    printHelp();
                    break;
                }
            }
        }
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("ECS CLIENT HELP (Usage):\r\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\r\n");
        sb.append(PROMPT).append("start");
        sb.append("\n\t\t\t\t Starts the storage service by calling start() on all KVServer instances " +
                "that participate in the service.\r\n");
        sb.append(PROMPT).append("stop");
        sb.append("\n\t\t\t\t Stops the service; all participating KVServers are stopped for processing client " +
                "requests " +
                "but the processes remain running.\r\n");
        sb.append(PROMPT).append("addNode <Cache Size> <Replacement Strategy>");
        sb.append("\n\t\t\t\t Create a new KVServer with the specified cache size and replacement strategy and " +
                "add it to the storage service at an arbitrary position.\r\n");
        sb.append(PROMPT).append("addNodes <# Nodes> <Cache Size> <Replacement Strategy>");
        sb.append("\n\t\t\t\t Randomly choose <numberOfNodes> servers from the available machines and start the " +
                "KVServer " +
                "by issuing an SSH call to the respective machine. This call launches the storage server with the " +
                "specified cache size and replacement strategy. For simplicity, locate the KVServer.jar in the same " +
                "directory as the ECS. All storage servers are initialized with the metadata and any persisted " +
                "data, and remain in state stopped.\r\n");
        sb.append(PROMPT).append("setupNodes <Nodes to Wait For> <Cache Strategy> <Cache Size>");
        sb.append("\n\t\t\t\t Wait for all nodes to report status or until timeout expires.\r\n");
        sb.append(PROMPT).append("awaitNodes <Nodes to Wait For> <Timeout>");
        sb.append("\n\t\t\t\t Removes nodes with names matching the nodeNames array.\r\n");
        sb.append(PROMPT).append("removeNodes [array of nodes] e.g. <node1> <node2> <node3> ...");
        sb.append("\n\t\t\t\t Remove a server from the storage service at an arbitrary position. \r\n");
        sb.append(PROMPT).append("getNodes");
        sb.append("\n\t\t\t\t Get a map of all nodes.\r\n");
        sb.append(PROMPT).append("getNodeByKey <key>");
        sb.append("\n\t\t\t\t Get the specific node responsible for the given key.\r\n");
        sb.append(PROMPT).append("logLevel");
        sb.append("\n\t\t\t\t changes the logLevel: ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \r\n");

        sb.append(PROMPT).append("quit ");
        sb.append("\n\t\t\t\t exits the program");
        System.out.println(sb.toString());
    }

    private void printPossibleLogLevels() {
        System.out.println(PROMPT
                + "Possible log levels are:");
        System.out.println(PROMPT
                + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }

    private String setLevel(String levelString) {

        if (levelString.equals(Level.ALL.toString())) {
            logger.setLevel(Level.ALL);
            return Level.ALL.toString();
        } else if (levelString.equals(Level.DEBUG.toString())) {
            logger.setLevel(Level.DEBUG);
            return Level.DEBUG.toString();
        } else if (levelString.equals(Level.INFO.toString())) {
            logger.setLevel(Level.INFO);
            return Level.INFO.toString();
        } else if (levelString.equals(Level.WARN.toString())) {
            logger.setLevel(Level.WARN);
            return Level.WARN.toString();
        } else if (levelString.equals(Level.ERROR.toString())) {
            logger.setLevel(Level.ERROR);
            return Level.ERROR.toString();
        } else if (levelString.equals(Level.FATAL.toString())) {
            logger.setLevel(Level.FATAL);
            return Level.FATAL.toString();
        } else if (levelString.equals(Level.OFF.toString())) {
            logger.setLevel(Level.OFF);
            return Level.OFF.toString();
        }
        return null;
    }

    private void printError(String error) {
        System.out.println(PROMPT + "Error! " + error);
    }

    private Object[] getArguments(String[] arguments, ArgType[] types) {
        Object[] array = new Object[types.length];
        int i = 1;
        try {
            for (ArgType type : types) {
                switch (type) {
                    case INTEGER:
                        array[i - 1] = Integer.parseInt(arguments[i++]);
                        break;
                    case STRING:
                        array[i - 1] = arguments[i++];
                        break;
                }
            }
        } catch (Exception e) {
            printHelp();
            return null;
        }
        return array;
    }

}
