package app_kvServer;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import common.messages.RequestResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;

import static common.messages.KVMessage.StatusType;


/**
 * Represents a connection end point for a particular client that is
 * connected to the server. This class is responsible for message reception
 * and sending.
 * The class also implements the KV_store functionality. Thus whenever a message
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
     * @param kvServer server
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
        OutputStreamWriter outputStreamWriter;

        try {
            inputStream = clientSocket.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedInputStream = new BufferedReader(inputStreamReader);
            outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream());

            while (clientSocketOpen) {
                try {
                    String reqLine;
                    while ((reqLine = bufferedInputStream.readLine()) != null) {
                        RequestResponse response = handleRequest(reqLine);

                        Gson gson = new Gson();
                        outputStreamWriter.write(gson.toJson(response, RequestResponse.class) + "\r\n");
                        outputStreamWriter.flush();

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

    /**
     * Handles request and request validation
     *
     * @return Response to send back to server
     */
    private RequestResponse handleRequest(String reqLine) throws IOException {

        RequestResponse request = null;
        RequestResponse response;

        Gson gson = new Gson();
        try {
            // deserialize string into a request and pass it off to handle it
            request = gson.fromJson(reqLine, RequestResponse.class);
            if (validateRequest(request)) {
                switch (request.getStatus()) {
                    case PUT:
                        try {
                            boolean keyExistInStorage = kvServer.inStorage(request.getKey());
                            boolean writeModifyDeleteStatus = kvServer.putKVWithError(request.getKey(), request
                                    .getValue());

                            // If the user is trying to delete
                            if (StringUtils.isEmpty(request.getValue())) {
                                if (writeModifyDeleteStatus) {
                                    logger.info("delete success");
                                    return new RequestResponse(request.getId(), request.getKey(), null, StatusType
                                            .DELETE_SUCCESS);
                                } else {
                                    logger.info("delete error");
                                    return new RequestResponse(request.getId(), request.getKey(), null, StatusType
                                            .DELETE_ERROR);
                                }
                            }
                            // if user is trying to modify or write new -/- status is true when new field or false
                            // when update
                            if (writeModifyDeleteStatus) {
                                logger.info("write success");
                                return new RequestResponse(request.getId(), request.getKey(), request.getValue(),
                                        StatusType.PUT_SUCCESS);
                            } else {
                                logger.info("modify success");
                                return new RequestResponse(request.getId(), request.getKey(), request.getValue(),
                                        StatusType.PUT_UPDATE);
                            }


                        } catch (IOException e) {
                            logger.error("Unable to get value from cache/disk - " + e.getMessage());
                            return new RequestResponse(-1, null, null, StatusType.SERVER_ERROR);
                        }

                    case GET:
                        try {
                            String value = kvServer.getKV(request.getKey());
                            if (value != null) {
                                logger.info("get success");
                                return new RequestResponse(request.getId(), request.getKey(), value, StatusType.GET_SUCCESS);
                            } else {
                                logger.info("get error");
                                return new RequestResponse(request.getId(), request.getKey(), value, StatusType.GET_ERROR);
                            }
                        } catch (IOException e) {
                            logger.error("Unable to get value from cache/disk - " + e.getMessage());
                            return new RequestResponse(-1, null, null, StatusType.SERVER_ERROR);
                        }
                }
            }
        } catch (JsonSyntaxException jsonException) {
            logger.error("Unable to parse JSON Request");
        } finally {
            response = new RequestResponse(-1, null, null, StatusType.INVALID_REQUEST);
        }

        return response;
    }

    /**
     * Validates requests
     *
     * @return true if request are good to proceed with otherwise false
     */
    private boolean validateRequest(RequestResponse request) {
        // if status is not get or put, send invalid request
        if (request.getStatus() != StatusType.GET && request.getStatus() != StatusType.PUT) {
            logger.error("Unknown request");
            return false;
        }

        // sanity check for get
        if (request.getStatus() == StatusType.GET) {
            if (StringUtils.isEmpty(request.getKey()) || request.getValue() != null) {
                logger.error("Invalid GET request");
                return false;
            }
        }

        // sanity check for put
        if (request.getStatus() == StatusType.PUT) {
            if (StringUtils.isEmpty(request.getKey())) {
                logger.error("Invalid put request");
                return false;
            }
        }

        return true;
    }


}