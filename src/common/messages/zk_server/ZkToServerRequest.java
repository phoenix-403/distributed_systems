package common.messages.zk_server;

import java.util.Arrays;
import java.util.List;

public class ZkToServerRequest {

    private int id;
    private ZkServerCommunication.Request zkSvrRequest;
    private List<String> nodes;

    // used for crash recovery!
    private String[] crashedServerHashRange;

    public ZkToServerRequest(int id, ZkServerCommunication.Request zkSvrRequest, List<String> nodes, String[]
            crashedServerHashRange) {
        this.id = id;
        this.zkSvrRequest = zkSvrRequest;
        this.nodes = nodes;
        this.crashedServerHashRange = crashedServerHashRange;
    }

    public int getId() {
        return id;
    }

    public String[] getCrashedServerHashRange() {
        return crashedServerHashRange;
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

    @Override
    public String toString() {
        return "ZkToServerRequest{" +
                "id=" + id +
                ", zkSvrRequest=" + zkSvrRequest +
                ", nodes=" + nodes +
                ", crashedServerHashRange=" + Arrays.toString(crashedServerHashRange) +
                '}';
    }
}
