package common.messages.zk_server;

import java.util.List;

public class ZkToServerRequest {

    private int id;
    private ZkServerCommunication.Request zkSvrRequest;
    private List<String> nodes;

    public ZkToServerRequest(int id, ZkServerCommunication.Request zkSvrRequest, List<String> nodes) {
        this.id = id;
        this.zkSvrRequest = zkSvrRequest;
        this.nodes = nodes;
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

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }
}
