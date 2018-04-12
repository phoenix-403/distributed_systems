package common.messages.zk_server;

import java.net.Socket;
import java.util.List;

public class ZkToServerResponse {

    private int id;
    private String serverName;
    private ZkServerCommunication.Response zkSvrResponse;
    private List<String> clientInfo=null;

    public ZkToServerResponse(int id, String serverName, ZkServerCommunication.Response zkSvrResponse) {
        this.id = id;
        this.serverName = serverName;
        this.zkSvrResponse = zkSvrResponse;
    }

    public ZkToServerResponse(int id, String serverName, ZkServerCommunication.Response zkSvrResponse, List<String> clientInfo) {
        this.id = id;
        this.serverName = serverName;
        this.zkSvrResponse = zkSvrResponse;
        this.clientInfo = clientInfo;
    }

    public int getId() {
        return id;
    }

    public String getServerName() {
        return serverName;
    }

    public ZkServerCommunication.Response getZkSvrResponse() {
        return zkSvrResponse;
    }

    public List<String> getClientInfo() {
        return clientInfo;
    }
}
