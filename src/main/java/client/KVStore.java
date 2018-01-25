package client;

import com.google.gson.Gson;
import common.messages.KVMessage;
import common.messages.Request;
import common.messages.Response;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;

public class KVStore implements KVCommInterface {


	private static Logger logger = LogManager.getLogger(KVStore.class);
	private static final String PROMPT = "KV_Client> ";
	private BufferedReader stdin;
	private Client client = null;
	private boolean stop = false;

	private String serverAddress;
	private int serverPort;

	public KVStore(String address, int port) {
		serverAddress = address;
		serverPort = port;
	}

	public boolean isRunning(){
	    return client.isRunning();
    }

	@Override
	public void connect() throws Exception{
        client = new Client(serverAddress, serverPort);
        client.start();
//        System.out.println(client.getMessage()); //todo @Henry this blocks
	}

	@Override
	public void disconnect() {
        if(client != null) {
            client.closeConnection();
            client = null;
        }
	}

	@Override
	public KVMessage put(String key, String value) throws Exception{
        Request req = new Request(0, key, value, KVMessage.StatusType.PUT);

        client.sendMessage(new Gson().toJson(req));
        KVMessage resp =getKVMessage();
        System.out.println( new Gson().toJson(resp));
        return resp;
    }

	@Override
	public KVMessage get(String key) throws Exception{
        Request req = new Request(0, key, null, KVMessage.StatusType.GET);

        client.sendMessage(new Gson().toJson(req));
        KVMessage resp =getKVMessage();
        System.out.println( new Gson().toJson(resp));
        return resp;
	}

    private KVMessage getKVMessage(){ return new Gson().fromJson(client.getMessage(), Response.class);}

	private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
	}
}
