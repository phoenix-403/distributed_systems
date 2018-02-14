package ecs;

public interface IECSNode {

    /**
     * @return  the name of the node (ie "Server 8.8.8.8")
     */
    String getNodeName();

    /**
     * @return  the hostname of the node (ie "8.8.8.8")
     */
    String getNodeHost();

    /**
     * @return  the port number of the node (ie 8080)
     */
    int getNodePort();

    /**
     * @return  array of two strings representing the low and high range of the hashes that the given node is responsible for
     */
    String[] getNodeHashRange();

}
