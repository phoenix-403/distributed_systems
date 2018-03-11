package test;

import common.helper.ConsistentHash;
import common.messages.Metadata;
import ecs.ECSNode;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetadataTest {

    List<ECSNode> ecsNodes = new ArrayList<>();

    ECSNode ecsNode1;
    ECSNode ecsNode2;
    ECSNode ecsNode3;
    ECSNode ecsNode4;
    @Before
    public void setup() {
        ecsNode1 = new ECSNode("server0", "localhost", 50000, new String[]{"05eaa8ab2a10954744c21574cd83e7f8",
                "2b786438d2c6425dc30de0077ea6494d"}, true);
        ecsNode2 = new ECSNode("server1", "localhost", 50001, new String[]{"3f870be1e85c8da20a2ffdf82afc3314",
                "0221f85727f09bb279fa843d25c48052"}, true);
        ecsNode3 = new ECSNode("server2", "localhost", 50002, new
                String[]{"0221f85727f09bb279fa843d25c48053", "05eaa8ab2a10954744c21574cd83e7f7"}, true);
        ecsNode4 = new ECSNode("server3", "localhost", 50003, new
                String[]{"2b786438d2c6425dc30de0077ea6494e", "3f870be1e85c8da20a2ffdf82afc3313"}, true);

        ecsNodes.add(ecsNode1);
        ecsNodes.add(ecsNode2);
        ecsNodes.add(ecsNode3);
        ecsNodes.add(ecsNode4);

    }

    @Test
    public void testSort(){
        Metadata metadata = new Metadata(ecsNodes);
        List<ECSNode> sortedEcsNodes = metadata.sortNodes(ecsNodes);
        assert (sortedEcsNodes.get(0) == ecsNode3);
        assert (sortedEcsNodes.get(1) == ecsNode1);
        assert (sortedEcsNodes.get(2) == ecsNode4);
        assert (sortedEcsNodes.get(3) == ecsNode2);
    }

    @Test
    public void test_getNextServer(){
        Metadata metadata = new Metadata(ecsNodes);
        metadata.sortNodes(ecsNodes);
        assert(metadata.getNextServer("server0", new ArrayList<>(Arrays.asList("server0","server1","server3"))) == ecsNode3
);
    }




}
