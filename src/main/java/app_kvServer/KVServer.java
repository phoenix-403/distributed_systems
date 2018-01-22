package main.java.app_kvServer;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class KVServer implements IKVServer {

    private static Logger logger = Logger.getRootLogger();

    private int port;
    private int cacheSize;
    private CacheStrategy cacheStrategy;

    private InetAddress inetAddress;

    private ServerSocket serverSocket;
    private boolean serverRunning;

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
    public KVServer(int port, int cacheSize, String strategy) {

        // saving server variables
        this.port = port;
        this.cacheSize = cacheSize;
        this.cacheStrategy = CacheStrategy.valueOf(strategy);

        // getting host info
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // todo, log this
            logger.error("Unknown host exception:\n" + e.getMessage());
        }

        // Initializing the server
        logger.info("Attempting to initialize server...");
        serverRunning = false;
        try {
            serverSocket = new ServerSocket(port);
            logger.info("listening on port: " + serverSocket.getLocalPort());

        } catch (IOException e) {
            logger.error("Error! Cannot open server socket:\n" + e.getMessage() );
        }
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean inCache(String key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getKV(String key) throws Exception {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public void putKV(String key, String value) throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void clearCache() {
        // TODO Auto-generated method stub
    }

    @Override
    public void clearStorage() {
        // TODO Auto-generated method stub
    }

    @Override
    public void run() {



    }

    @Override
    public void kill() {
        serverRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + port, e);
        }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new Exception("Incorrect server arguments!");
        }

        KVServer server = new KVServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), args[2]);

    }

}
