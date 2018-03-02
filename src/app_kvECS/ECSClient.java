package app_kvECS;

import common.helper.Script;
import common.helper.ZkConnector;
import common.helper.ZkNodeTransaction;
import ecs.ECSNode;
import ecs.IECSNode;
import ecs.ZkStructureNodes;
import logger.LogSetup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
    private static final String PROMPT = "ECS_Client> ";
    private boolean stopClient = false;

    private String zkAddress;
    private int zkPort;

    private ZooKeeper zooKeeper;
    private ZkConnector zkConnector;
    private ZkNodeTransaction zkNodeTransaction;

    private List<ECSNode> ecsNodes = new ArrayList<>();

    enum ArgType {
        INTEGER,
        STRING,
    }

    private ECSClient(String configFile) throws IOException, EcsException {

        // setting up log
        try {
            new LogSetup("logs/ecs/ecs_client.log", Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // setting zookeeper variables
        zkAddress = "localhost";
        zkPort = 2181;

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
        zkNodeTransaction.createZNode(ZkStructureNodes.GLOBAL_STATUS.getValue(), null, CreateMode.PERSISTENT);
        zkNodeTransaction.createZNode(ZkStructureNodes.HEART_BEAT.getValue(), null, CreateMode.PERSISTENT);
        zkNodeTransaction.createZNode(ZkStructureNodes.METADATA.getValue(), null, CreateMode.PERSISTENT);
        zkNodeTransaction.createZNode(ZkStructureNodes.SERVER_NODES.getValue(), null, CreateMode.PERSISTENT);
    }

    private void stopZK() throws InterruptedException {
        zkConnector.close();
        String zkStopScript = System.getProperty("user.dir") + "/src/app_kvECS/stopZK.sh";
        runScript(zkStopScript, logger);
    }


    @Override
    public boolean start() {
        // TODO
        return false;
    }

    @Override
    public boolean stop() {
        // TODO
        return false;
    }

    @Override
    public boolean shutdown() {
        // TODO
        return false;
    }

    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        // TODO
        ArrayList<IECSNode> list = (ArrayList<IECSNode>) addNodes(1, cacheStrategy, cacheSize);
        return list.get(0);
    }

    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {

        ArrayList<IECSNode> ecsNodes = (ArrayList<IECSNode>) setupNodes(count, cacheStrategy, cacheSize);

        // Launch the server processes
        for (IECSNode ecsNode : ecsNodes) {
            createRunSshScript((ECSNode) ecsNode, cacheStrategy, cacheSize);
        }

        // call await nodes to wait for processes to start
        boolean success = awaitNodes(count, 60000);
        // update node to added by setting boolean in ecs node

        return ecsNodes;
    }

    private void createRunSshScript(ECSNode ecsNode, String cacheStrategy, int cacheSize) {

        //todo fix metadata
        String scriptContent = "ssh -n " +
                "abdelrahman@localhost " +
                "nohup java -jar ~/IdeaProjects/distributed_systems/m2-server.jar " +
                ecsNode.getNodeName() + " " +
                zkAddress + " " +
                zkPort + " " +
                ecsNode.getNodePort() + " " +
                cacheSize + " " +
                cacheStrategy + " " +
                "metadata " +
                "&";
        String scriptPath = System.getProperty("user.dir") + "/src/app_kvECS/ssh.sh";

        createBashScript(scriptPath, scriptContent, logger);
        Script.runScript(scriptPath, logger);

    }

    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        List<IECSNode> nodesToSetup = getUnreservedNodes();
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

        // marking new nodes as reserved
        for (IECSNode ecsNode : nodesToSetup) {
            ((ECSNode) ecsNode).setReserved(true);
        }
        // todo m1: wait on zi input to calculate metadata using ECS_NODES not NODES TO SETUP ... m2: ecs deleted
        // need to merge :(


        return nodesToSetup;
    }

    private ArrayList<IECSNode> getUnreservedNodes() {
        List<IECSNode> availableNodes = new ArrayList<>(ecsNodes);
        CollectionUtils.filter(availableNodes, ecsNode -> !((ECSNode) ecsNode).isReserved());
        return (ArrayList<IECSNode>) availableNodes;
    }


    @Override
    public boolean awaitNodes(int count, int timeout) {
        // TODO
        return false;
    }

    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        // TODO
        return false;
    }

    @Override
    public Map<String, IECSNode> getNodes() {
        // TODO
        return null;
    }

    @Override
    public IECSNode getNodeByKey(String Key) {
        // TODO
        return null;
    }

    private void run() throws EcsException {
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

    private void handleCommand(String cmdLine) throws EcsException {
        String[] tokens = cmdLine.split("\\s+");
        Arrays.stream(tokens)
                .filter(s -> s != null && s.length() > 0
                ).collect(Collectors.toList()).toArray(tokens);

        if (tokens.length != 0 && tokens[0] != null) {
            switch (tokens[0]) {
                case "start": {
                    start();
                    break;
                }
                case "stopClient": {
                    stop();
                    break;
                }
                case "shutdown": {
                    shutdown();
                    break;
                }
                case "addNode": {
                    Object[] a = getArguments(tokens, new ArgType[]{STRING, INTEGER});
                    if (a == null)
                        return;
                    IECSNode node = addNode((String) a[0], (int) a[1]);
                    break;
                }
                case "addNodes": {
                    Object[] a = getArguments(tokens, new ArgType[]{INTEGER, STRING, INTEGER});
                    if (a == null)
                        return;
                    Collection<IECSNode> nodes = addNodes((int) a[0], (String) a[1], (int) a[2]);
                    break;
                }
                case "setupNodes": {
                    Object[] a = getArguments(tokens, new ArgType[]{INTEGER, STRING, INTEGER});
                    if (a == null)
                        return;
                    Collection<IECSNode> nodes = setupNodes((int) a[0], (String) a[1], (int) a[2]);
                    break;
                }
                case "awaitNodes": {
                    Object[] a = getArguments(tokens, new ArgType[]{INTEGER, INTEGER});
                    if (a == null)
                        return;
                    try {
                        awaitNodes((int) a[0], (int) a[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
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

    // todo @ Henry ... update this
    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("ECS CLIENT HELP (Usage):\r\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\r\n");
        sb.append(PROMPT).append("connect <host> <port>");
        sb.append("\t establishes a connection to a server\r\n");
        sb.append(PROMPT).append("put <key> <value>");
        sb.append("\t\t Inserts a key-value pair into the storage server data structures.\r\n" +
                "\t\t\t\t\t\t\t\t\t Updates (overwrites) the current value with the given value if the server " +
                "already contains the specified key.\r\n" +
                "\t\t\t\t\t\t\t\t\t Deletes the entry for the given key if <value> is null.\r\n");
        sb.append(PROMPT).append("get <key>");
        sb.append("\t\t\t\t Retrieves the value for the given key from the storage server. \r\n");
        sb.append(PROMPT).append("disconnect");
        sb.append("\t\t\t\t disconnects from the server \r\n");

        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t\t\t changes the logLevel: ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \r\n");

        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t\t\t exits the program");
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
                        array[i - 1] = new Integer(Integer.parseInt(arguments[i++]));
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


    /**
     * @param args contains the port number at args[0].
     *             Main entry point for the KV client application.
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

}
