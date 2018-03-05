package common.messages;

public class ZkToServerRequest {

    private int id;
    private ZkServerCommunication.Request zkSvrRequest;

    public ZkToServerRequest(int id, ZkServerCommunication.Request zkSvrRequest) {
        this.id = id;
        this.zkSvrRequest = zkSvrRequest;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ZkServerCommunication.Request getZkSvrRequest() {
        return zkSvrRequest;
    }

    public void setZkSvrRequest(ZkServerCommunication.Request zkSvrRequest) {
        this.zkSvrRequest = zkSvrRequest;
    }
}
