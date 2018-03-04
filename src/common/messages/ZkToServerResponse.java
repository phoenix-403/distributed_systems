package common.messages;

public class ZkToServerResponse {

    private String serverName;
    private ZkServerCommunication.Response zkSvrResponse;

    public ZkToServerResponse(String serverName, ZkServerCommunication.Response zkSvrResponse) {
        this.serverName = serverName;
        this.zkSvrResponse = zkSvrResponse;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public ZkServerCommunication.Response getZkSvrResponse() {
        return zkSvrResponse;
    }

    public void setZkSvrResponse(ZkServerCommunication.Response zkSvrResponse) {
        this.zkSvrResponse = zkSvrResponse;
    }
}
