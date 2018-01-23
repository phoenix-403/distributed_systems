package client;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import common.messages.KVMessage;
import common.messages.Request;

public class KVStore implements KVCommInterface, ClientSocketListener{
	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	private static Logger logger = Logger.getRootLogger();
	private static final String PROMPT = "EchoClient> ";
	private BufferedReader stdin;
	private Client client = null;
	private boolean stop = false;

	private String serverAddress;
	private int serverPort;

	public KVStore(String address, int port) {
		serverAddress = address;
		serverPort = port;
	}

	@Override
	public void connect() {
        try {
            client = new Client(serverAddress, serverPort);
        } catch (IOException e) {
            printError("Unable to connect");
        }
        client.addListener(this);
        client.start();
	}

	@Override
	public void disconnect() {
        if(client != null) {
            client.closeConnection();
            client = null;
        }
	}

	@Override
	public KVMessage put(String key, String value) {
		// TODO Auto-generated method stub
        if(client != null && client.isRunning()){
            Request req = new Request(124, key, value, KVMessage.StatusType.PUT);

            sendMessage(new Gson().toJson(req));
        } else {
            printError("Not connected!");
        }
        return null;
    }

	@Override
	public KVMessage get(String key) {
        // TODO Auto-generated method stub
        if(client != null && client.isRunning()){
            Request req = new Request(125, key, null, KVMessage.StatusType.GET);

            sendMessage(new Gson().toJson(req));
        } else {
            printError("Not connected!");
        }
        return null;
	}

	@Override
	public void handleNewMessage(TextMessage msg) {
		if(!stop) {
			System.out.println(msg.getMsg());
			System.out.print(PROMPT);
		}
	}

	@Override
	public void handleStatus(SocketStatus status) {
		if(status == SocketStatus.CONNECTED) {

		} else if (status == SocketStatus.DISCONNECTED) {
			System.out.print(PROMPT);
			System.out.println("Connection terminated: "
					+ serverAddress + " / " + serverPort);

		} else if (status == SocketStatus.CONNECTION_LOST) {
			System.out.println("Connection lost: "
					+ serverAddress + " / " + serverPort);
			System.out.print(PROMPT);
		}

	}

    private void sendMessage(String msg){
        try {
            client.sendMessage(new TextMessage(msg));
        } catch (IOException e) {
//            printError("Unable to send message!");
            disconnect();
        }
    }

	private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
	}
}
