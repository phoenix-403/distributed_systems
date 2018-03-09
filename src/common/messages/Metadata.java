package common.messages;

import common.helper.ConsistentHash;
import ecs.ECSNode;

import java.util.ArrayList;
import java.util.List;

public class Metadata {

    private List<ECSNode> ecsNodes = new ArrayList<>();

    public static final String MAX_MD5 = "ffffffffffffffffffffffffffffffff";
    public static final String MIN_MD5 = "00000000000000000000000000000000";


    public Metadata(List<ECSNode> ecsNodes) {
        List<ECSNode> temp = new ArrayList<>(ecsNodes);
        for (ECSNode ecsNode : temp) {
            if (ecsNode.isReserved()) {
                this.ecsNodes.add(ecsNode);
            }
        }
    }


    public List<ECSNode> getEcsNodes() {
        return ecsNodes;
    }

    public boolean isWithinRange(String key, String nodeName) {
        String hashKey = ConsistentHash.getMD5(key);
        for (ECSNode ecsNode : ecsNodes) {
            if (nodeName.equals(ecsNode.getNodeName())) {
                if (ecsNode.getNodeHashRange()[0].compareTo(ecsNode.getNodeHashRange()[1]) > 0) {
                    if ((hashKey.compareTo(ecsNode.getNodeHashRange()[0]) >= 0
                            && hashKey.compareTo(MAX_MD5) <= 0) ||

                            (hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0
                                    && hashKey.compareTo(MIN_MD5) >= 0)) {
                        return true;
                    }
                } else {
                    if (hashKey.compareTo(ecsNode.getNodeHashRange()[0]) >= 0
                            && hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // used for testing
    public boolean isHashWithinRange(String hashKey, String nodeName) {
        for (ECSNode ecsNode : ecsNodes) {
            if (nodeName.equals(ecsNode.getNodeName())) {
                if (ecsNode.getNodeHashRange()[0].compareTo(ecsNode.getNodeHashRange()[1]) > 0) {
                    if ((hashKey.compareTo(ecsNode.getNodeHashRange()[0]) >= 0
                            && hashKey.compareTo(MAX_MD5) <= 0) ||

                            (hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0
                                    && hashKey.compareTo(MIN_MD5) >= 0)) {
                        return true;
                    }
                } else {
                    if (hashKey.compareTo(ecsNode.getNodeHashRange()[0]) >= 0
                            && hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public ECSNode getResponsibleServer(String key) {
        String hashKey = ConsistentHash.getMD5(key);
        for (ECSNode ecsNode : ecsNodes) {
            if (ecsNode.getNodeHashRange()[0].compareTo(ecsNode.getNodeHashRange()[1]) > 0) {
                if ((hashKey.compareTo(ecsNode.getNodeHashRange()[0]) >= 0
                        && hashKey.compareTo(MAX_MD5) <= 0) ||

                        (hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0
                                && hashKey.compareTo(MIN_MD5) >= 0)) {
                    return ecsNode;
                }
            } else {
                if (hashKey.compareTo(ecsNode.getNodeHashRange()[0]) >= 0
                        && hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0) {
                    return ecsNode;
                }
            }
        }

        // we should never reach here !!!!
        return null;
    }


}
