package ecs;

import client.KVStore;
import com.google.gson.Gson;
import com.sun.security.ntlm.Client;
import common.messages.KVMessage;
import common.messages.RequestResponse;
import org.apache.zookeeper.server.Request;

import java.io.*;
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

    public void lockWrite(String key) {
        RequestResponse req = new RequestResponse(requestId++, key, null, KVMessage.StatusType.WRITE_LOCK);
        boolean status = sendRequest(req);
        if (status) {
            RequestResponse response = getResponse();
        }
    }

    public void unLockWrite(String key) {
        RequestResponse req = new RequestResponse(requestId++, key, null, KVMessage.StatusType.WRITE_UNLOCK);
        boolean status = sendRequest(req);
        if (status) {
            RequestResponse response = getResponse();
        }
    }

    public void transferData(String key) {
        RequestResponse req = new RequestResponse(requestId++, key, null, KVMessage.StatusType.TRANSFER_DATA);
        boolean status = sendRequest(req);
        if (status) {
            RequestResponse response = getResponse();
        }
    }

    public boolean sendRequest(RequestResponse req) {
        Socket socket;
        try {
            socket = new Socket(nodeHost, nodePort);
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            outputStreamWriter.write(new Gson().toJson(req, RequestResponse.class) + "\r\n");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    private RequestResponse getResponse() {
        return null;
    }
}
