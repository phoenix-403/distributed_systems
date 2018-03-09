package ecs;

import com.google.gson.Gson;
import common.messages.ClientServerRequestResponse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;

public class ECSNode implements IECSNode{

    private String nodeName;
    private String nodeHost;
    private int nodePort;
    private String [] nodeHashRange;
    private boolean reserved;

    private int requestId = 0;
    private OutputStreamWriter outputStreamWriter;
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedInputStream;

    public ECSNode(String nodeName, String nodeHost, int nodePort, String[] nodeHashRange, boolean reserved) {
        this.nodeName = nodeName;
        this.nodeHost = nodeHost;
        this.nodePort = nodePort;
        this.nodeHashRange = nodeHashRange;
        this.reserved = reserved;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public String getNodeHost() {
        return nodeHost;
    }

    @Override
    public int getNodePort() {
        return nodePort;
    }

    @Override
    public String[] getNodeHashRange() {
        return nodeHashRange;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setNodeHashRange(String[] nodeHashRange) {
        this.nodeHashRange = nodeHashRange;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    @Override
    public String toString() {
        return "ECSNode{" +
                "nodeName='" + nodeName + '\'' +
                ", nodeHost='" + nodeHost + '\'' +
                ", nodePort=" + nodePort +
                ", nodeHashRange=" + Arrays.toString(nodeHashRange) +
                ", reserved=" + reserved +
                '}';
    }

//    public void lockWrite(String key) {
//        ClientServerRequestResponse req = new ClientServerRequestResponse(requestId++, key, null, KVMessage.StatusType.WRITE_LOCK, null);
//        boolean status = sendRequest(req);
//        if (status) {
//            ClientServerRequestResponse response = getResponse();
//        }
//    }
//
//    public void unLockWrite(String key) {
//        ClientServerRequestResponse req = new ClientServerRequestResponse(requestId++, key, null, KVMessage.StatusType.WRITE_UNLOCK, null);
//        boolean status = sendRequest(req);
//        if (status) {
//            ClientServerRequestResponse response = getResponse();
//        }
//    }
//
//    public void transferData(String key) {
//        ClientServerRequestResponse req = new ClientServerRequestResponse(requestId++, key, null, KVMessage.StatusType.TRANSFER_DATA, null);
//        boolean status = sendRequest(req);
//        if (status) {
//            ClientServerRequestResponse response = getResponse();
//        }
//    }

    public boolean sendRequest(ClientServerRequestResponse req) {
        Socket socket;
        try {
            socket = new Socket(nodeHost, nodePort);
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            outputStreamWriter.write(new Gson().toJson(req, ClientServerRequestResponse.class) + "\r\n");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    private ClientServerRequestResponse getResponse() {
        return null;
    }
}
