package common.messages.zk_server;

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

    public String getServerName() {
        return serverName;
    }


    public ZkServerCommunication.Response getZkSvrResponse() {
        return zkSvrResponse;
    }

    @Override
    public String toString() {
        return "{id:" + id + ",serverName:" + serverName + ",response:" + zkSvrResponse + "}";
    }
}
