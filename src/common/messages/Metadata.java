package common.messages;

import common.helper.ConsistentHash;
import ecs.ECSNode;

import java.util.ArrayList;
import java.util.List;

public class Metadata {

    private List<ECSNode> ecsNodes = new ArrayList<>();

    private final String MAX = "ffffffffffffffffffffffffffffffff";
    private final String MIN = "00000000000000000000000000000000";


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
                            && hashKey.compareTo(MAX) <= 0) ||

                            (hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0
                                    && hashKey.compareTo(MIN) >= 0)) {
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

    public boolean isHashWithinRange(String hashKey, String nodeName) {
        for (ECSNode ecsNode : ecsNodes) {
            if (nodeName.equals(ecsNode.getNodeName())) {
                if (ecsNode.getNodeHashRange()[0].compareTo(ecsNode.getNodeHashRange()[1]) > 0) {
                    if ((hashKey.compareTo(ecsNode.getNodeHashRange()[0]) >= 0
                            && hashKey.compareTo(MAX) <= 0) ||

                            (hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0
                                    && hashKey.compareTo(MIN) >= 0)) {
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
                        && hashKey.compareTo(MAX) <= 0) ||

                        (hashKey.compareTo(ecsNode.getNodeHashRange()[1]) <= 0
                                && hashKey.compareTo(MIN) >= 0)) {
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
