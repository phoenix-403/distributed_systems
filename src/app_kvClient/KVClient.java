package app_kvClient;

import client.KVCommInterface;
import client.KVStore;
import common.messages.KVMessage;
import common.messages.Metadata;
import logger.LogSetup;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class KVClient implements IKVClient, IClientSocketListener {

    private static Logger logger = LogManager.getLogger(KVClient.class);
    private static final String PROMPT = "KV_Client> ";
    private BufferedReader stdin;
    private KVStore defaultKvStoreInstance = null;
    private boolean stop = false;
    private ErrorMessage errM = new ErrorMessage();

    private String serverAddress;
    private int serverPort;

    private Metadata metadata = null;
    private HashMap<String, KVStore> allKVStores = new HashMap<>();

    @Override
    public void newConnection(String hostname, int port) throws Exception {
        serverAddress = hostname;
        serverPort = port;
        defaultKvStoreInstance = new KVStore(this, serverAddress, serverPort);
        defaultKvStoreInstance.set(this);
        defaultKvStoreInstance.connect();
    }

    @Override
    public KVCommInterface getStore() {
        return defaultKvStoreInstance;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public void setAllKVStores(HashMap<String, KVStore> allKVStores) {
        this.allKVStores = allKVStores;
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
        Arrays.stream(tokens).filter(s -> s != null && s.length() > 0).collect(Collectors.toList()).toArray(tokens);

        if (tokens.length != 0 && tokens[0] != null) {
            switch (tokens[0]) {
                case "quit":
                    stop = true;
                    if (defaultKvStoreInstance != null)
                        defaultKvStoreInstance.disconnect();
                    System.out.println(PROMPT + "Application exit!");
                    break;
                case "connect":
                    if (tokens.length == 3) {
                        if (defaultKvStoreInstance != null) {
                            printTerminal("Already connected. Disconnect first!");
                        } else {
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
                        }
                    } else {
                        printError("Invalid number of parameters!");
                    }
                    break;
                case "get":
                    if (defaultKvStoreInstance == null) {
                        errM.printNotConnectedError();
                        logger.warn("Not Connected");
                    } else {
                        if (errM.validateServerCommand(tokens, KVMessage.StatusType.GET)) {
                            try {
                                if (metadata == null) {
                                    defaultKvStoreInstance.get(tokens[1]);
                                } else {
                                    KVStore kvStore = allKVStores.get(metadata.getResponsibleServer(tokens[1])
                                            .getNodeName());
                                    if (!kvStore.isConnected()) {
                                        kvStore.connect();
                                    }
                                    kvStore.get(tokens[1]);
                                }
                            } catch (Exception e) {
                                if (e instanceof NullPointerException) {
                                    errM.printUnableToConnectError("Unable to connect to the required KV_store!");
                                } else {
                                    errM.printUnableToConnectError(e.getMessage());
                                    logger.warn("Connection lost!");
                                }
                            }
                        }
                    }
                    break;
                case "put":
                    if (defaultKvStoreInstance == null) {
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
                                if (metadata == null) {
                                    defaultKvStoreInstance.put(tokens[1], arg);
                                } else {
                                    KVStore kvStore = allKVStores.get(metadata.getResponsibleServer(tokens[1])
                                            .getNodeName());
                                    if (!kvStore.isConnected()) {
                                        kvStore.connect();
                                    }
                                    kvStore.put(tokens[1], arg);
                                }
                            } catch (Exception e) {
                                errM.printUnableToConnectError(e.getMessage());
                                logger.warn("Connection lost!");
                            }
                        }
                    }
                    break;
                case "disconnect":
                    if (defaultKvStoreInstance != null) {
                        // disconnecting default kv store
                        defaultKvStoreInstance.disconnect();
                        defaultKvStoreInstance = null;

                        // disconnecting all kv stores
                        KVStore kvStore;
                        for (String key : allKVStores.keySet()) {
                            kvStore = allKVStores.get(key);
                            if (kvStore.isConnected()) {
                                kvStore.disconnect();
                            }
                        }

                        // nullifying metadata
                        metadata = null;

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