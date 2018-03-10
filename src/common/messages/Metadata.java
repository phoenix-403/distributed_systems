package common.messages;

import common.helper.ConsistentHash;
import ecs.ECSNode;

import java.util.*;

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

    public List<ECSNode> sortNodes(List<ECSNode> ecsNodez){
        TreeMap<String, ECSNode> sortThis = new TreeMap<>();
        for (ECSNode ecsNode : ecsNodez) {
            sortThis.put(ecsNode.getNodeHashRange()[0], ecsNode);
        }
        List<ECSNode> sortedList = new ArrayList<>();
        Iterator it = sortThis.entrySet().iterator();
        while (it.hasNext()) {
            sortedList.add((ECSNode)((Map.Entry)it.next()).getValue());
        }

        return sortedList;
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
        String[] range = getRange(nodeName);
        return (hashKey.compareTo(range[1])) <= 0 && (hashKey.compareTo(range[0])) >=0;
    }

    public String[] getRange(String name) {
        for (ECSNode ecsNode : ecsNodes) {
            if (ecsNode.getNodeName().equals(name)) {
                return ecsNode.getNodeHashRange();
            }
        }
        return null;
    }

    public ECSNode getNextServer(String id, List<String> removed) {
        boolean currentPassed = false;
        ECSNode before = null;
        ECSNode after = null;
        List<ECSNode> sortedNodes = sortNodes(ecsNodes);
        for (ECSNode ecsNode : sortedNodes) {
            if (ecsNode.getNodeName().equals(id)) {
                currentPassed = true;
                continue;
            }
            if (!removed.contains(ecsNode.getNodeName()) && before == null && currentPassed == false)
                before = ecsNode;
            if (!removed.contains(ecsNode.getNodeName()) && after == null && currentPassed == true)
                after = ecsNode;
        }
        if (after!=null) {
            return after;
        } else
            return before;
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
