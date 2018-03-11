package client;

import app_kvClient.IClientSocketListener;
import app_kvClient.KVClient;
import com.google.gson.Gson;
import common.messages.client_server.ClientServerRequestResponse;
import common.messages.client_server.KVMessage;
import common.messages.Metadata;
import ecs.ECSNode;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class KVStore implements KVCommInterface {


    private static Logger logger = LogManager.getLogger(KVStore.class);

    private String address;
    private int port;

    private static final int TIMEOUT = 4 * 1000;

    private KVClient kvClient;

    private Socket clientSocket;
    private IClientSocketListener clientSocketListener;
    private OutputStreamWriter outputStreamWriter;
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedInputStream;

    private int requestId = 0;

    public KVStore(KVClient kvClient, String address, int port) {
        this.kvClient = kvClient;
        this.address = address;
        this.port = port;
    }

    @Override
    public void connect() throws Exception {
        clientSocket = new Socket(address, port);
        inputStream = clientSocket.getInputStream();
        inputStreamReader = new InputStreamReader(inputStream);
        bufferedInputStream = new BufferedReader(inputStreamReader);
        outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream());
    }

    @Override
    public void disconnect() {
        logger.info("try to close connection ...");
        try {
            tearDownConnection();
        } catch (IOException ioe) {
            logger.error("Unable to close connection properly! - " + ioe.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return clientSocket.isConnected();
    }

    private void tearDownConnection() throws IOException {
        logger.info("tearing down the connection ...");
        if (clientSocket != null) {
            inputStream.close();
            inputStreamReader.close();
            bufferedInputStream.close();
            outputStreamWriter.close();
            clientSocket.close();
            clientSocket = null;
            logger.info("disconnected from " + address + " port " + port);
        }
    }

    @Override
    public KVMessage put(String key, String value) throws IOException {
        ClientServerRequestResponse req = new ClientServerRequestResponse(requestId++, key, value, KVMessage
                .StatusType.PUT, null);
        boolean status = sendRequest(req);
        if (status) {
            ClientServerRequestResponse response = getResponse();
            if (KVMessage.StatusType.CONNECTION_DROPPED.equals(response.getStatus()))
                throw new IOException("Connection Dropped");
            return response;
        } else {
            throw new IOException("Not Connected");
        }

    }

    @Override
    public KVMessage get(String key) throws IOException {
        ClientServerRequestResponse req = new ClientServerRequestResponse(requestId++, key, null, KVMessage
                .StatusType.GET, null);
        boolean status = sendRequest(req);
        if (status) {
            ClientServerRequestResponse response = getResponse();
            if (KVMessage.StatusType.CONNECTION_DROPPED.equals(response.getStatus()))
                throw new IOException("Connection Dropped");
            return response;
        } else {
            throw new IOException("Not Connected");
        }
    }

    private boolean sendRequest(ClientServerRequestResponse req) {
        try {
            outputStreamWriter.write(new Gson().toJson(req, ClientServerRequestResponse.class) + "\r\n");
            outputStreamWriter.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    private ClientServerRequestResponse getResponse() {
        try {
            ClientServerRequestResponse response;

            long startTime = System.currentTimeMillis();

            String respLine;
            while (System.currentTimeMillis() - startTime < TIMEOUT
                    && (respLine = bufferedInputStream.readLine()) != null) {

                Gson gson = new Gson();
                response = gson.fromJson(respLine, ClientServerRequestResponse.class);
                // updating metadata if needed
                if (response.getStatus().equals(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE)) {
                    updateMetadata(response.getMetadata());
                }
                // sending response to terminal
                if (clientSocketListener != null) {
                    clientSocketListener.printTerminal(response.toString());
                    logResponse(response);
                }
                return response;

            }

            response = new ClientServerRequestResponse(-1, null, null, KVMessage.StatusType.TIME_OUT, null);
            if (clientSocketListener != null) {
                clientSocketListener.printTerminal(response.toString());
                logResponse(response);
            }
            return response;

        } catch (IOException e) {
            return connectionDropped();
        }
    }

    private void updateMetadata(Metadata metadata) {
        kvClient.setMetadata(metadata);
        HashMap<String, KVStore> kvStoreHashMap = new HashMap<>();
        KVStore kvStore;
        for (ECSNode ecsNode : metadata.getEcsNodes()) {
            kvStore = new KVStore(kvClient, ecsNode.getNodeHost(), ecsNode.getNodePort());
            try {
                kvStore.connect();
                kvStore.set(kvClient);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            kvStoreHashMap.put(ecsNode.getNodeName(), kvStore);
        }
        kvClient.setAllKVStores(kvStoreHashMap);
    }

    private void logResponse(ClientServerRequestResponse response) {
        if (response.getStatus().equals(KVMessage.StatusType.GET_ERROR)
                || response.getStatus().equals(KVMessage.StatusType.PUT_ERROR)
                || response.getStatus().equals(KVMessage.StatusType.DELETE_ERROR)
                || response.getStatus().equals(KVMessage.StatusType.SERVER_ERROR))
            logger.error(response.toString());
        else
            logger.info(response.toString());
    }

    private ClientServerRequestResponse connectionDropped() {
        ClientServerRequestResponse response = new ClientServerRequestResponse(-1, null, null,
                KVMessage.StatusType.CONNECTION_DROPPED, null);
        if (clientSocketListener != null)
            clientSocketListener.printTerminal(response.toString());
        return response;
    }

    public void set(IClientSocketListener listener) {
        clientSocketListener = listener;
    }
}
