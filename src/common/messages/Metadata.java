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

    public List<ECSNode> sortNodes(List<ECSNode> ecsNodez) {
        TreeMap<String, ECSNode> sortThis = new TreeMap<>();
        for (ECSNode ecsNode : ecsNodez) {
            sortThis.put(ecsNode.getNodeHashRange()[0], ecsNode);
        }


        List<ECSNode> sortedList = new ArrayList<>();
        Iterator it = sortThis.entrySet().iterator();
        while (it.hasNext()) {
            sortedList.add((ECSNode) ((Map.Entry) it.next()).getValue());
        }

        return sortedList;
    }


    public List<ECSNode> getEcsNodes() {
        return ecsNodes;
    }

    public boolean isWithinRange(String key, String nodeName) {
        if (getResponsibleServer(key).getNodeName().equals(nodeName)) {
            return true;
        }
        return false;
    }

    public boolean isWithinRange(String hashKey, String[] range) {
        if (range[0].compareTo(range[1]) > 0) {
            if ((hashKey.compareTo(range[0]) >= 0
                    && hashKey.compareTo(MAX_MD5) <= 0) ||

                    (hashKey.compareTo(range[1]) <= 0
                            && hashKey.compareTo(MIN_MD5) >= 0)) {
                return true;
            }
        } else {
            if (hashKey.compareTo(range[0]) >= 0
                    && hashKey.compareTo(range[1]) <= 0) {
                return true;
            }
        }
        return false;
    }

    // used for testing
    public boolean isHashWithinRange(String hashKey, String nodeName) {
        String[] range = getRange(nodeName);
        return (hashKey.compareTo(range[1])) <= 0 && (hashKey.compareTo(range[0])) >= 0;
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
        if (ecsNodes.size() == removed.size()) {
            return null;
        }

        boolean currentPassed = false;
        ECSNode before = null;
        ECSNode after = null;
        List<ECSNode> sortedNodes = sortNodes(ecsNodes);
        for (ECSNode ecsNode : sortedNodes) {
            if (ecsNode.getNodeName().equals(id)) {
                currentPassed = true;
                continue;
            }
            if (!removed.contains(ecsNode.getNodeName()) && before == null && !currentPassed)
                before = ecsNode;
            if (!removed.contains(ecsNode.getNodeName()) && after == null && currentPassed)
                after = ecsNode;
        }
        if (after != null) {
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

    public static void main(String[] args) {
        // todo add this as a unit test case
        List<ECSNode> ecsNodes = new ArrayList<>();
        ECSNode ecsNode1 = new ECSNode("server0", "localhost", 50000, new String[]{"05eaa8ab2a10954744c21574cd83e7f8",
                "2b786438d2c6425dc30de0077ea6494d"}, true);
        ECSNode ecsNode2 = new ECSNode("server1", "localhost", 50001, new String[]{"3f870be1e85c8da20a2ffdf82afc3314",
                "0221f85727f09bb279fa843d25c48052"}, true);
        ECSNode ecsNode3 = new ECSNode("server2", "localhost", 50002, new
                String[]{"0221f85727f09bb279fa843d25c48053", "05eaa8ab2a10954744c21574cd83e7f7"}, true);
        ECSNode ecsNode4 = new ECSNode("server3", "localhost", 50003, new
                String[]{"2b786438d2c6425dc30de0077ea6494e", "3f870be1e85c8da20a2ffdf82afc3313"}, true);


        ecsNodes.add(ecsNode1);
        ecsNodes.add(ecsNode2);
        ecsNodes.add(ecsNode3);
        ecsNodes.add(ecsNode4);

        Metadata metadata = new Metadata(ecsNodes);
        List<ECSNode> sortedEcsNodes = metadata.sortNodes(ecsNodes); // correct sorting .. s2, s0, s3 --> s1
        // (wraparoundNode)
        System.out.println(sortedEcsNodes);


        List<ECSNode> sortedCopy = new ArrayList<>(sortedEcsNodes);
        System.out.println(metadata.getNextServer("server0", new ArrayList<>(Arrays.asList("server0","server1","server3"))));

    }


}
