package app_kvClient;

import app_kvECS.EcsException;
import client.KVCommInterface;
import client.KVStore;
import com.google.gson.Gson;
import common.messages.ClientServerRequestResponse;
import common.messages.KVMessage;
import common.messages.Metadata;
import ecs.ECSNode;
import logger.LogSetup;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KVClient implements IKVClient, IClientSocketListener {

    private static Logger logger = LogManager.getLogger(KVClient.class);
    private static final String PROMPT = "KV_Client> ";
    private BufferedReader stdin;
    private KVStore kvStoreInstance = null;
    private HashMap<String, KVStore> allKVStores;
    private boolean stop = false;
    private ErrorMessage errM = new ErrorMessage();
    private Metadata metadata = null;
    private String configFile = "ecs.config";

    private String serverAddress;
    private int serverPort;

    @Override
    public void newConnection(String hostname, int port) throws Exception {
        serverAddress = hostname;
        serverPort = port;
        kvStoreInstance = new KVStore(serverAddress, serverPort);
        kvStoreInstance.set(this);
        kvStoreInstance.connect();

    }

    @Override
    public KVCommInterface getStore() {
        return kvStoreInstance;
    }

    public void run() {

        try {
            new LogSetup("logs/client/client.log", Level.ALL);
            connectAll();
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
                .filter(s -> s != null && s.length() > 0
                ).collect(Collectors.toList()).toArray(tokens);

        if (tokens.length != 0 && tokens[0] != null) {
            switch (tokens[0]) {
                case "quit":
                    stop = true;
                    if (kvStoreInstance != null)
                        kvStoreInstance.disconnect();
                    System.out.println(PROMPT + "Application exit!");
                    break;
                case "connect":
                    if (tokens.length == 3) {
                        try {
                            newConnection(tokens[1], Integer.parseInt(tokens[2]));
                            printTerminal("Connected!");
                        } catch (NumberFormatException nfe) {
                            printError("No valid address. Port must be a number!");
                            logger.error("Unable to parse argument <port>", nfe);
                        } catch (Exception e) {
                            errM.printUnableToConnectError(e.getMessage());
                            logger.error("Unable to connect - ", e);
                        }
                    } else {
                        printError("Invalid number of parameters!");
                    }
                    break;
                case "get":
//                    if (!preliminaryCheck(tokens[1])) {
                    get(tokens);
                    break;
                case "test":
                    Iterator it = allKVStores.entrySet().iterator();
                    while (it.hasNext())
                        try {
                            ((KVStore) ((Map.Entry) it.next()).getValue()).testMetadata();
                        } catch (Exception e) {

                        }
                    break;
                case "put":
                    put(tokens);
                    break;
                case "disconnect":
                    if (kvStoreInstance != null) {
                        kvStoreInstance.disconnect();
                        kvStoreInstance = null;
                        printTerminal("Disconnected!");
                        logger.info("Disconnected");
                    } else {
                        printTerminal("Nothing to disconnect from");
                        logger.warn("Attempting to disconnect from nothing!");
                    }
                    break;
                case "logLevel":
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
                case "help":
                    printHelp();
                    break;
                default:
                    printError("Unknown command");
                    printHelp();
                    break;
            }
        }
    }

    private void checkConnections(ECSNode node){
        if (node == null) {
            logger.error("No Servers Running");
        } else if (allKVStores.get(node.getNodeName()) == null) {
            connectAll();
        } else if (!allKVStores.get(node.getNodeName()).isConnected()) {
            connectAll();
        }
    }

    private void put(String[] tokens) {
        if (kvStoreInstance == null) {
            errM.printNotConnectedError();
            logger.warn("Not Connected");
        } else {
            if (errM.validateServerCommand(tokens, KVMessage.StatusType.PUT)) {
                try {
                    String arg = tokens.length <= 2 ? null : tokens[2];
                    if (arg != null) {
                        for (int i = 3; i < tokens.length; i++) {
                            arg += (" " + tokens[i]);
                        }
                    }
                    KVMessage msg = new ClientServerRequestResponse(0 , null, null,
                            KVMessage.StatusType.SERVER_NOT_RESPONSIBLE);
                    while (msg.getStatus().equals(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE)) {
                        ECSNode node = metadata.getResponsibleServer(tokens[1]);
                        checkConnections(node);
                        msg = allKVStores.get(node.getNodeName()).put(tokens[1], arg);
                        updateMetadata(msg.getValue());
                    }
                } catch (Exception e) {
                    errM.printUnableToConnectError(e.getMessage());
                    logger.warn("Connection lost!");
                }
            }
        }
    }

    private void get(String[] tokens) {
        if (!true) {
            errM.printNotConnectedError();
            logger.warn("Not Connected to Responsible Server");
            logger.info("Updating Connections and Metadata ...");
            connectAll();
        } else {
            if (errM.validateServerCommand(tokens, KVMessage.StatusType.GET)) {
                try {
                    KVMessage msg = new ClientServerRequestResponse(0 , null, null,
                            KVMessage.StatusType.SERVER_NOT_RESPONSIBLE);
                    while (msg.getStatus().equals(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE)) {
                        ECSNode node = metadata.getResponsibleServer(tokens[1]);
                        checkConnections(node);
                        msg = allKVStores.get(node.getNodeName()).get(tokens[1]);
                        updateMetadata(msg.getValue());
                    }
                } catch (Exception e) {
                    errM.printUnableToConnectError(e.getMessage());
                    logger.warn("Connection lost!");
                }
            }
        }
    }

    private void connectAll() {
        allKVStores = new HashMap<>();

        File file = new File("src/app_kvECS/" + configFile);

        final String DELIMITER = " ";
        final String DELIMITER_PATTERN = Pattern.quote(DELIMITER);

        ArrayList<String> fileLines = null;
        try {
            fileLines = (ArrayList<String>) Files.readAllLines(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String line : fileLines) {
            String[] tokenizedLine = line.split(DELIMITER_PATTERN);
            try {
                KVStore store = new KVStore(tokenizedLine[1], Integer.parseInt(tokenizedLine[2]));
                store.connect();
                allKVStores.put(tokenizedLine[0],
                        store);
                logger.info("Connected to " + tokenizedLine[0]);
            } catch (NumberFormatException nfe) {
                printError("No valid address. Port must be a number!");
                logger.error("Unable to parse argument <port>", nfe);
            } catch (Exception e) {
                logger.info("Unable to Reach " + tokenizedLine[0]);
            }
        }
    }


//        return allKVStores.get(metadata.getResponsibleServer(key).getNodeHost()).isConnected();
//    }

    private void updateMetadata(String jsonData) {
        metadata = new Gson().fromJson(jsonData, Metadata.class);
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("KV CLIENT HELP (Usage):\r\n");
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

    @Override
    public void printTerminal(String msg) {
        System.out.println(PROMPT + msg);
        System.out.flush();
    }

    /**
     * @param args contains the port number at args[0].
     *             Main entry point for the KV client application.
     */
    public static void main(String[] args) {
        KVClient app = new KVClient();
        app.run();
    }
}
