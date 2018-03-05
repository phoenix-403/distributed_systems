package common.messages;

public class ZkToServerResponse {

    private int id;
    private String serverName;
    private ZkServerCommunication.Response zkSvrResponse;

    public ZkToServerResponse(int id, String serverName, ZkServerCommunication.Response zkSvrResponse) {
        this.id = id;
        this.serverName = serverName;
        this.zkSvrResponse = zkSvrResponse;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
