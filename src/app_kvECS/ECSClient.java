package app_kvECS;

import app_kvClient.ErrorMessage;
import app_kvClient.KVClient;
import client.KVCommInterface;
import client.KVStore;
import common.messages.KVMessage;
import ecs.IECSNode;
import logger.LogSetup;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ECSClient implements IECSClient {

    private static Logger logger = LogManager.getLogger(ECSClient.class);
    private static final String PROMPT = "ECS_Client> ";
    private BufferedReader stdin;
    private boolean stop = false;

    private String serverAddress;
    private int serverPort;

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
        return null;
    }

    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        // TODO
        return null;
    }

    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        // TODO
        return null;
    }

    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
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

    public void run() {
        try {
            new LogSetup("logs/client/client.log", Level.ALL);
        } catch (IOException e) {
            System.out.println("unable to initialize client logger");
            e.printStackTrace();
            System.exit(-1);
        }
        while (!stop) {
            stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);

            try {
                String cmdLine = stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                stop = true;
                printError("CLI does not respond - Application terminated ");
            }
        }
    }

    private void handleCommand(String cmdLine) {
        String[] tokens = cmdLine.split("\\s+");
        Arrays.stream(tokens)
                .filter(new Predicate<String>() {
                            @Override
                            public boolean test(String s) {
                                return s != null && s.length() > 0;
                            }
                        }
                ).collect(Collectors.toList()).toArray(tokens);

        if (tokens.length != 0 && tokens[0] != null) {
            switch (tokens[0]) {
                case "start": {
                    
                    start();
                    break;
                } case "stop": {
                    
                    stop();
                    break;
                } case "shutdown": {
                    
                    shutdown();
                    break;
                } case "addNode": {
                    
                    IECSNode node = addNode(tokens[1], Integer.parseInt(tokens[2]));
                    break;
                } case "addNodes": {
                    
                    Collection<IECSNode> nodes = addNodes(Integer.parseInt(tokens[1]), tokens[2], Integer.parseInt(tokens[3]));
                    break;
                } case "setupNodes": {
                    
                    Collection<IECSNode> nodes = setupNodes(Integer.parseInt(tokens[1]), tokens[2], Integer.parseInt(tokens[3]));
                    break;
                } case "awaitNodes": {
                    
                    try {
                        awaitNodes(Integer.parseInt(tokens[1]),Integer.parseInt(tokens[2]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                } case "removeNodes": {
                    
                    ArrayList<String> temp = new ArrayList<String>(
                            Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length)));
                    removeNodes(temp);
                    break;
                } case "getNodes": {
                    
                    Map<String, IECSNode> nodes = getNodes();
                    break;
                } case "getNodeByKey": {
                    
                    IECSNode node = getNodeByKey(tokens[1]);
                    break;
                } case "logLevel": {
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
                } case "help": {
                    printHelp();
                    break;
                } default: {
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

    public void printTerminal(String msg) {
        System.out.println(PROMPT + msg);
        System.out.flush();
    }

    /**
     * @param args contains the port number at args[0].
     *             Main entry point for the KV client application.
     */
    public static void main(String[] args) {
        ECSClient app = new ECSClient();
        app.run();
    }
}
