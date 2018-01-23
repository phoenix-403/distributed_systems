package app_kvServer;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import common.messages.Request;
import common.messages.Response;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;


/**
 * Represents a connection end point for a particular client that is
 * connected to the server. This class is responsible for message reception
 * and sending.
 * The class also implements the echo functionality. Thus whenever a message
 * is received it is going to be echoed back to the client.
 */
public class ClientConnection implements Runnable {

    private static Logger logger = LogManager.getLogger(ClientConnection.class);

    private KVServer kvServer;

    private Socket clientSocket;
    private boolean clientSocketOpen;

    /**
     * Constructs a new ClientConnection object for a given TCP socket.
     *
     * @param kvServer
     * @param clientSocket the Socket object for the client connection.
     */
    ClientConnection(KVServer kvServer, Socket clientSocket) {
        this.kvServer = kvServer;
        this.clientSocket = clientSocket;
        this.clientSocketOpen = true;
    }

    /**
     * Initializes and starts the client connection.
     * Loops until the connection is closed or aborted by the client.
     */
    public void run() {

        // variables related to receiving data from client
        BufferedReader bufferedInputStream = null;
        InputStreamReader inputStreamReader = null;
        InputStream inputStream;

        try {
            inputStream = clientSocket.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedInputStream = new BufferedReader(inputStreamReader);

            while (clientSocketOpen) {
                try {
                    String reqLine;
                    while ((reqLine = bufferedInputStream.readLine()) != null) {
                        Response response = handleRequest(reqLine);
                    }

                    /* connection either terminated by the client or lost due to
                     * network problems*/
                } catch (IOException ioe) {
                    logger.info("Connection lost with " + clientSocket.getInetAddress().getHostName() + "!");
                    clientSocketOpen = false;
                }
            }

        } catch (IOException ioe) {
            logger.error("Error! Connection could not be established!", ioe);

        } finally {

            try {
                if (clientSocket != null) {
                    if (bufferedInputStream != null)
                        bufferedInputStream.close();
                    if (inputStreamReader != null)
                        inputStreamReader.close();
                    if (bufferedInputStream != null)
                        bufferedInputStream.close();
                    clientSocket.close();
                }
            } catch (IOException ioe) {
                logger.error("Error! Unable to tear down connection!", ioe);
            }
        }
    }

    private Response handleRequest(String reqLine) throws IOException {

        Response response;

        Gson gson = new Gson();
        try {
            // deserializes string into a request and pass it off to handle it
            Request request = gson.fromJson(reqLine, Request.class);
            if (validateRequest(request)) {

                switch (request.getStatus()) {
                    case PUT:

                        try {
                            kvServer.putKV(request.getKey(), request.getValue());
                        } catch (Exception e) {
                            // Todo return appropriate error and log
                            e.printStackTrace();
                        }

                        break;
                    case GET:
                        try {
                            kvServer.getKV(request.getKey());
                        } catch (Exception e) {
                            // Todo return appropriate error and log
                            e.printStackTrace();
                        }
                        break;
                }
            }


        } catch (JsonSyntaxException jsonException) {

        } finally {
            // To do fix this to return actual response
            response = new Response(0, null, null, null);
        }

        return response;
    }

    private boolean validateRequest(Request request) {
        // validate request logistics;
        return true;
    }


}
