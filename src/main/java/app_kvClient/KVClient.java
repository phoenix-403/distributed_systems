package app_kvClient;

import client.KVCommInterface;
import client.KVStore;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import common.messages.KVMessage;

public class KVClient implements IKVClient {
    @Override
    public void newConnection(String hostname, int port) throws Exception{
        // TODO Auto-generated method stub
    }

    @Override
    public KVCommInterface getStore(){
        // TODO Auto-generated method stub
        return null;
    }

    private static Logger logger = Logger.getRootLogger();
    private static final String PROMPT = "EchoClient> ";
    private BufferedReader stdin;
    private KVStore KVinstance = null;
    private boolean stop = false;

    private String serverAddress;
    private int serverPort;

    public void run() {
        while(!stop) {
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


        if(tokens[0].equals("quit")) {
            stop = true;
            KVinstance.disconnect();
            System.out.println(PROMPT + "Application exit!");

        } else if (tokens[0].equals("connect")){
            if(tokens.length == 3) {
                try{
                    serverAddress = tokens[1];
                    serverPort = Integer.parseInt(tokens[2]);
                    KVinstance = new KVStore(serverAddress, serverPort);
                    KVinstance.connect();
//					connect(serverAddress, serverPort);
                } catch(NumberFormatException nfe) {
                    printError("No valid address. Port must be a number!");
                    logger.info("Unable to parse argument <port>", nfe);
                }
            } else {
                printError("Invalid number of parameters!");
            }
        } else if (tokens[0].equals("get")) {
            if (tokens.length <2) {
                printError("No value passed!");
            } else if (tokens[1].length() > 20) {
                printError("Key exceeds 20B!");
            } else {
                if (KVinstance == null) {
                    printError("Connect First");
                } else {
                    try {
                        KVinstance.get(tokens[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        } else if (tokens[0].equals("put")) {
            if (tokens.length > 3) {
                printError("Too many arguments!");
            } else if (tokens.length <3) {
                printError("Too few arguments!");
            } if (tokens[1].length() > 20) {
                printMessage(KVMessage.StatusType.PUT_ERROR.toString());
            } else if (tokens[2].length() > 120*1024) {
                printError("Value exceeds 120KB");
            } else if(tokens.length == 3) {
                if (KVinstance == null) {
                    printError("Connect First");
                } else {
                    try {
                        KVinstance.put(tokens[1], tokens[2]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                printError("No value passed!");
            }

        } else if(tokens[0].equals("disconnect")) {
            KVinstance.disconnect();

        } else if(tokens[0].equals("logLevel")) {
            if(tokens.length == 2) {
                String level = setLevel(tokens[1]);
                if(level.equals(null)) {
                    printError("No valid log level!");
                    printPossibleLogLevels();
                } else {
                    System.out.println(PROMPT +
                            "Log level changed to level " + level);
                }
            } else {
                printError("Invalid number of parameters!");
            }

        } else if(tokens[0].equals("help")) {
            printHelp();
        } else {
            printError("Unknown command");
            printHelp();
        }
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("ECHO CLIENT HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("connect <host> <port>");
        sb.append("\t establishes a connection to a server\n");
        sb.append(PROMPT).append("put <key> <value>");
        sb.append("\t\t Inserts a key-value pair into the storage server data structures.|" +
                " Updates (overwrites) the current value with the given value if the server " +
                "already contains the specified key.|" +
                " Deletes the entry for the given key if <value> equals null.\n \n");
        sb.append(PROMPT).append("get <key>");
        sb.append("\t\t Retrieves the value for the given key from the storage server. \n");
        sb.append(PROMPT).append("disconnect");
        sb.append("\t\t\t disconnects from the server \n");

        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t changes the logLevel \n");
        sb.append(PROMPT).append("\t\t\t\t ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");

        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t exits the program");
        System.out.println(sb.toString());
    }

    private void printPossibleLogLevels() {
        System.out.println(PROMPT
                + "Possible log levels are:");
        System.out.println(PROMPT
                + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }

    private String setLevel(String levelString) {

        if(levelString.equals(Level.ALL.toString())) {
            logger.setLevel(Level.ALL);
            return Level.ALL.toString();
        } else if(levelString.equals(Level.DEBUG.toString())) {
            logger.setLevel(Level.DEBUG);
            return Level.DEBUG.toString();
        } else if(levelString.equals(Level.INFO.toString())) {
            logger.setLevel(Level.INFO);
            return Level.INFO.toString();
        } else if(levelString.equals(Level.WARN.toString())) {
            logger.setLevel(Level.WARN);
            return Level.WARN.toString();
        } else if(levelString.equals(Level.ERROR.toString())) {
            logger.setLevel(Level.ERROR);
            return Level.ERROR.toString();
        } else if(levelString.equals(Level.FATAL.toString())) {
            logger.setLevel(Level.FATAL);
            return Level.FATAL.toString();
        } else if(levelString.equals(Level.OFF.toString())) {
            logger.setLevel(Level.OFF);
            return Level.OFF.toString();
        }
        return null;
    }

    private void printError(String error){
        System.out.println(PROMPT + "Error! " +  error);
    }

    private void printMessage(String message){
        System.out.println(PROMPT + message);
    }

    /**
     * Main entry point for the echo server application.
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
        try {
            new LogSetup("logs/client.log", Level.OFF);
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
        KVClient app = new KVClient();
            app.run();
    }
}
