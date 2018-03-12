package test;

import common.helper.ConsistentHash;
import common.messages.Metadata;
import ecs.ECSNode;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsistentHashTest extends TestCase{

    private List<ECSNode> ecsNodeList;
    private ConsistentHash consistentHash;

    private ECSNode serv0;
    private ECSNode serv1;
    private ECSNode serv2;
    private ECSNode serv3;
    private ECSNode serv4;
    private ECSNode serv5;
    private ECSNode serv6;
    private ECSNode serv7;
    private ECSNode serv8;
    private ECSNode serv9;


// ECSNode{nodeName='server0', nodeHashRange=[228bebd6d6e35c8ee716a3416dcaf578, 2b786438d2c6425dc30de0077ea6494d]}
// WA ECSNode{nodeName='server1', nodeHashRange=[ec9d4f0c674d5487dad5c79cb0d4ee24, 05eaa8ab2a10954744c21574cd83e7f7]}
// ECSNode{nodeName='server2', nodeHashRange=[b87c6da95b39edc845134fb118f001ab, ec9d4f0c674d5487dad5c79cb0d4ee23]}
// ECSNode{nodeName='server3', nodeHashRange=[2b786438d2c6425dc30de0077ea6494e, 33b0bff3dfa3064ca9d93c282fc67deb]}
// ECSNode{nodeName='server4', nodeHashRange=[974a769d5ea802b77c2713f5fea21f50, b87c6da95b39edc845134fb118f001aa]}
// ECSNode{nodeName='server5', nodeHashRange=[5d51e183fa04afd7e84ae74967b98b1d, 6a4997ef87eaa5951bf2fd21ffab145a]}
// ECSNode{nodeName='server6', nodeHashRange=[05eaa8ab2a10954744c21574cd83e7f8, 1f94ea955766f22d41d4aeda0c722f74]}
// ECSNode{nodeName='server8', nodeHashRange=[33b0bff3dfa3064ca9d93c282fc67dec, 5d51e183fa04afd7e84ae74967b98b1c]}
// ECSNode{nodeName='server7', nodeHashRange=[1f94ea955766f22d41d4aeda0c722f75, 228bebd6d6e35c8ee716a3416dcaf577]}
// ECSNode{nodeName='server9', nodeHashRange=[6a4997ef87eaa5951bf2fd21ffab145b, 974a769d5ea802b77c2713f5fea21f4f]}

    @Test
    public void testHash() {
        assert (ConsistentHash.getMD5("yoyo").equals("48dc8d29308eb256edc76f25def07251"));
    }

    @Test
    public void testEveryServerWithAKey() {
        serv0 = new ECSNode("server0", "localhost", 50000, null, true);
        serv1 = new ECSNode("server1", "localhost", 50002, null, true);
        serv2 = new ECSNode("server2", "localhost", 50004, null, true);
        serv3 = new ECSNode("server3", "localhost", 50006, null, true);
        serv4 = new ECSNode("server4", "localhost", 50008, null, true);
        serv5 = new ECSNode("server5", "localhost", 50010, null, true);
        serv6 = new ECSNode("server6", "localhost", 50012, null, true);
        serv7 = new ECSNode("server7", "localhost", 50014, null, true);
        serv8 = new ECSNode("server8", "localhost", 50016, null, true);
        serv9 = new ECSNode("server9", "localhost", 50018, null, true);

        ecsNodeList = new ArrayList<>(Arrays.asList(serv0, serv1, serv2, serv3, serv4, serv5, serv6, serv7, serv8,
                serv9));

        consistentHash = new ConsistentHash(ecsNodeList);


        consistentHash.hash();
        Metadata metadata = new Metadata(ecsNodeList);
        assert (metadata.isHashWithinRange("228bebd6d6e35c8ee716a3416dcaf578", serv0.getNodeName()));
        assert (metadata.isHashWithinRange("258bebd6d6e35c8ee716a3416dcaf578", serv0.getNodeName()));
        assert (metadata.isHashWithinRange("2b786438d2c6425dc30de0077ea6494d", serv0.getNodeName()));

        assert (metadata.isHashWithinRange("ec9d4f0c674d5487dad5c79cb0d4ee24", serv1.getNodeName()));
        assert (metadata.isHashWithinRange("fc9d4f0c674d5487dad5c79cb0d4ee24", serv1.getNodeName()));
        assert (metadata.isHashWithinRange("ffffffffffffffffffffffffffffffff", serv1.getNodeName()));
        assert (metadata.isHashWithinRange("00000000000000000000000000000000", serv1.getNodeName()));
        assert (metadata.isHashWithinRange("04000000000000000000000000000000", serv1.getNodeName()));
        assert (metadata.isHashWithinRange("05eaa8ab2a10954744c21574cd83e7f7", serv1.getNodeName()));

        assert (metadata.isHashWithinRange("b87c6da95b39edc845134fb118f001ab", serv2.getNodeName()));
        assert (metadata.isHashWithinRange("cc9d4f0c674d5487dad5c79cb0d4ee23", serv2.getNodeName()));
        assert (metadata.isHashWithinRange("ec9d4f0c674d5487dad5c79cb0d4ee23", serv2.getNodeName()));

        assert (metadata.isHashWithinRange("2b786438d2c6425dc30de0077ea6494e", serv3.getNodeName()));
        assert (metadata.isHashWithinRange("2f786438d2c6425dc30de0077ea6494e", serv3.getNodeName()));
        assert (metadata.isHashWithinRange("33b0bff3dfa3064ca9d93c282fc67deb", serv3.getNodeName()));

        assert (metadata.isHashWithinRange("974a769d5ea802b77c2713f5fea21f50", serv4.getNodeName()));
        assert (metadata.isHashWithinRange("b07c6da95b39edc845134fb118f001aa", serv4.getNodeName()));
        assert (metadata.isHashWithinRange("b87c6da95b39edc845134fb118f001aa", serv4.getNodeName()));

        assert (metadata.isHashWithinRange("5d51e183fa04afd7e84ae74967b98b1d", serv5.getNodeName()));
        assert (metadata.isHashWithinRange("5f51e183fa04afd7e84ae74967b98b1d", serv5.getNodeName()));
        assert (metadata.isHashWithinRange("6a4997ef87eaa5951bf2fd21ffab145a", serv5.getNodeName()));

        assert (metadata.isHashWithinRange("05eaa8ab2a10954744c21574cd83e7f8", serv6.getNodeName()));
        assert (metadata.isHashWithinRange("1094ea955766f22d41d4aeda0c722f74", serv6.getNodeName()));
        assert (metadata.isHashWithinRange("1f94ea955766f22d41d4aeda0c722f74", serv6.getNodeName()));

        assert (metadata.isHashWithinRange("1f94ea955766f22d41d4aeda0c722f75", serv7.getNodeName()));
        assert (metadata.isHashWithinRange("200bebd6d6e35c8ee716a3416dcaf577", serv7.getNodeName()));
        assert (metadata.isHashWithinRange("228bebd6d6e35c8ee716a3416dcaf577", serv7.getNodeName()));

        assert (metadata.isHashWithinRange("33b0bff3dfa3064ca9d93c282fc67dec", serv8.getNodeName()));
        assert (metadata.isHashWithinRange("3fb0bff3dfa3064ca9d93c282fc67dec", serv8.getNodeName()));
        assert (metadata.isHashWithinRange("5d51e183fa04afd7e84ae74967b98b1c", serv8.getNodeName()));

        assert (metadata.isHashWithinRange("6a4997ef87eaa5951bf2fd21ffab145b", serv9.getNodeName()));
        assert (metadata.isHashWithinRange("904a769d5ea802b77c2713f5fea21f4f", serv9.getNodeName()));
        assert (metadata.isHashWithinRange("974a769d5ea802b77c2713f5fea21f4f", serv9.getNodeName()));

    }


}
