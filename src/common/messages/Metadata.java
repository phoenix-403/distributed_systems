package common.messages;

import ecs.ECSNode;

import java.util.List;

public class Metadata {

    List<ECSNode> ecsNodes;


    public Metadata (List<ECSNode> ecsNodes) {
        this.ecsNodes = ecsNodes;
    }


    public List<ECSNode> getEcsNodes() {
        return ecsNodes;
    }

    public void addNodes(List<ECSNode> ecsNodes){

    }

    public void removeNodes(List<ECSNode> ecsNodes){

    }

    public ECSNode getServer(String key) {
//        MetadataEntry closest= map.get(0);
//        for(Map.Entry<String,MetadataEntry> entry : map.entrySet()) {
//            if (key.compareTo(closest.getHashRange()) >= 0
//                    && (entry.getValue().getHashRange().compareTo(closest.getHashRange()) -
//                    entry.getValue().getHashRange().compareTo(closest.getHashRange())) > 0
//            ) {
//
//            }
//            Integer value = entry.getValue();
//
//            System.out.println(key + " => " + value);
//        }
//        return closest;
        //TODO after Chewbaka finishes wrap around
        return null;
    }


}
