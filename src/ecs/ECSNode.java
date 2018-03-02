package ecs;

public class ECSNode implements IECSNode{

    private String nodeName;
    private String nodeHost;
    private int nodePort;
    private String [] nodeHashRange;
    private boolean reserved;

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
}
