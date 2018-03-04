package common.messages;

public class ZkToServerRequest {

    private ZkServerCommunication.Request zkSvrRequest;

    public ZkToServerRequest(ZkServerCommunication.Request zkSvrRequest) {
        this.zkSvrRequest = zkSvrRequest;
    }

    public ZkServerCommunication.Request getZkSvrRequest() {
        return zkSvrRequest;
    }

    public void setZkSvrRequest(ZkServerCommunication.Request zkSvrRequest) {
        this.zkSvrRequest = zkSvrRequest;
    }
}
