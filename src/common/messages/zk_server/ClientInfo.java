package common.messages.zk_server;

import app_kvServer.ClientConnection;

import java.net.ServerSocket;
import java.util.List;

public class ClientInfo {
    List<ClientConnection> clientData;
    int reqID;
    ServerSocket socket;
    ZkServerCommunication.Response responseState;

    public ClientInfo(List<ClientConnection> clientData, int reqID, ServerSocket socket,
                      ZkServerCommunication.Response responseState) {
        this.clientData = clientData;
        this.reqID = reqID;
        this.socket = socket;
        this.responseState = responseState;
    }

    public List<ClientConnection> getClientData() {
        return clientData;
    }

    public void setClientData(List<ClientConnection> clientData) {
        this.clientData = clientData;
    }

    public int getReqID() {
        return reqID;
    }

    public void setReqID(int reqID) {
        this.reqID = reqID;
    }

    public ZkServerCommunication.Response getResponseState() {
        return responseState;
    }

    public void setResponseState(ZkServerCommunication.Response responseState) {
        this.responseState = responseState;
    }
}
