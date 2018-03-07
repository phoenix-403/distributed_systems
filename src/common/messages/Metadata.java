package common.messages;

import common.helper.ConsistentHash;
import ecs.ECSNode;

import java.util.ArrayList;
import java.util.List;

public class Metadata {

    private List<ECSNode> ecsNodes = new ArrayList<>();


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


    public ECSNode getResponsibleServer(String key) {
        String hashKey = ConsistentHash.getMD5(key);
        for (ECSNode ecsNode : ecsNodes) {
            if (hashKey.compareTo(ecsNode.getNodeHashRange()[0]) <= 0
                    && hashKey.compareTo(ecsNode.getNodeHashRange()[1]) >= 0) {
                return ecsNode;
            }
        }
        // we should never reach here !!!!
        return null;
    }


}
