package client;

import app_kvClient.IClientSocketListener;
import com.google.gson.Gson;
import common.messages.KVMessage;
import common.messages.Request;
import common.messages.Response;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class KVStore implements KVCommInterface {


    private static Logger logger = LogManager.getLogger(KVStore.class);

    private String address;
    private int port;

    private static final int TIMEOUT = 4 * 1000;

    private Socket clientSocket;
    private IClientSocketListener clientSocketListener;
    private OutputStreamWriter outputStreamWriter;
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedInputStream;

    private int requestId = 0;

    public KVStore(String address, int port) {
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
        logger.info("Connection established");

    }

    @Override
    public void disconnect() {
        logger.info("try to close connection ...");
        try {
            tearDownConnection();
        } catch (IOException ioe) {
            logger.error("Unable to close connection!");
        }
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
            logger.info("connection closed!");
        }
    }

    @Override
    public KVMessage put(String key, String value) throws IOException {
        if ("null".equals(value)){
            value = null;
        }
        Request req = new Request(requestId++, key, value, KVMessage.StatusType.PUT);
        boolean status = sendRequest(req);
        if (status) {
            Response response = getResponse();
            if (KVMessage.StatusType.CONNECTION_DROPPED.equals(response.getStatus()))
                throw new IOException("Connection Dropped");
            return response;
        } else {
            throw new IOException("Not Connected");
        }

    }

    @Override
    public KVMessage get(String key) throws IOException {
        Request req = new Request(requestId++, key, null, KVMessage.StatusType.GET);
        boolean status = sendRequest(req);
        if (status) {
            Response response = getResponse();
            if (KVMessage.StatusType.CONNECTION_DROPPED.equals(response.getStatus()))
                throw new IOException("Connection Dropped");
            return response;
        } else {
            throw new IOException("Not Connected");
        }
    }

    private boolean sendRequest(Request req) {
        try {
            outputStreamWriter.write(new Gson().toJson(req, Request.class) + "\r\n");
            outputStreamWriter.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    private Response getResponse() {
        try {
            Response response;

            long startTime = System.currentTimeMillis();

            String respLine;
            while (System.currentTimeMillis() - startTime < TIMEOUT
                    && (respLine = bufferedInputStream.readLine()) != null) {

                Gson gson = new Gson();
                response = gson.fromJson(respLine, Response.class);
                if (clientSocketListener != null)
                    clientSocketListener.printTerminal(response.toString());
                return response;

            }

            response = new Response(-1, null, null, KVMessage.StatusType.TIME_OUT);
            if (clientSocketListener != null)
                clientSocketListener.printTerminal(response.toString());
            return response;

        } catch (IOException e) {
            return connectionDropped();
        }
    }

    private Response connectionDropped() {
        Response response = new Response(-1, null, null,
                KVMessage.StatusType.CONNECTION_DROPPED);
        if (clientSocketListener != null)
            clientSocketListener.printTerminal(response.toString());
        return response;
    }

    public void set(IClientSocketListener listener) {
            clientSocketListener = listener;
    }
}
