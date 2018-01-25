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

    volatile boolean active;

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
            clientSocket.getOutputStream().close();
            clientSocket.close();
            clientSocket = null;
            logger.info("connection closed!");
        }
    }

    @Override
    public KVMessage put(String key, String value) throws Exception {
        Request req = new Request(requestId++, key, value, KVMessage.StatusType.PUT);
        sendRequest(req);
        return getResponse();
    }

    @Override
    public KVMessage get(String key) throws Exception {
        Request req = new Request(requestId++, key, null, KVMessage.StatusType.GET);
        sendRequest(req);
        return getResponse();
    }

    private void sendRequest(Request req) throws IOException {
        outputStreamWriter.write(new Gson().toJson(req, Request.class) + "\r\n");
        outputStreamWriter.flush();
    }


    private Response getResponse() throws IOException {

        Response response;

        long startTime = System.currentTimeMillis();

        String respLine;
        while (System.currentTimeMillis() - startTime < TIMEOUT
                && (respLine = bufferedInputStream.readLine()) != null) {

            Gson gson = new Gson();
            response = gson.fromJson(respLine, Response.class);
            clientSocketListener.printTerminal(response.toString());
            return response;

        }

        response = new Response(-1, null, null, KVMessage.StatusType.TIME_OUT);
        clientSocketListener.printTerminal(response.toString());
        return response;
    }

    public void set(IClientSocketListener listener) {
        clientSocketListener = listener;
    }
}
