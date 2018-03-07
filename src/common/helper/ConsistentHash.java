package common.helper;

import ecs.ECSNode;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class ConsistentHash {

    private List<ECSNode> ecsNodes;
    private List<IDHashPair> idHashPairs;
    private TreeMap<String, String[]> idHashRangePairs;

    public ConsistentHash(List<ECSNode> ecsNodes) {
        this.ecsNodes = ecsNodes;
        idHashPairs = new ArrayList<>();
        idHashRangePairs = new TreeMap<>();
    }

    public void hash() {
        String nodeName;
        String nodeAddress;

        for (ECSNode ecsNode : ecsNodes) {
            // only hash active servers
            if (ecsNode.isReserved()) {
                nodeName = ecsNode.getNodeName();
                nodeAddress = ecsNode.getNodeHost() + ":" + ecsNode.getNodePort();
                idHashPairs.add(new IDHashPair(nodeName, getMD5(nodeAddress)));
            }
        }

        storeHashRange();

        // updating ecsNodes hash range
        for (ECSNode ecsNode : ecsNodes) {
            if (ecsNode.isReserved()) {
                ecsNode.setNodeHashRange(idHashRangePairs.get(ecsNode.getNodeName()));
            }
        }
    }


    public static String getMD5(String preImage) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(preImage.getBytes("UTF-8"));
            BigInteger number = new BigInteger(1, messageDigest);
//            System.out.println("number: " + number);
            StringBuilder hashText = new StringBuilder(number.toString(16));
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
//            System.out.println("string: " + hashText.toString());
            return hashText.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void storeHashRange() {
        Collections.sort(idHashPairs);
        for (int i = 0; i < idHashPairs.size(); i++) {
            String[] hashRanges = new String[2];
            if (i > 0) {
                BigInteger value = new BigInteger(idHashPairs.get(i - 1).getHashVal(), 16);
                value = value.add(BigInteger.ONE);
                StringBuilder start = new StringBuilder(value.toString(16));
                // Now we need to zero pad it if you actually want the full 32 chars.
                while (start.length() < 32) {
                    start.insert(0, "0");
                }
                hashRanges[0] = start.toString();
                hashRanges[1] = idHashPairs.get(i).getHashVal();
            } else {
                if (idHashPairs.get(idHashPairs.size() - 1).getHashVal().equals("ffffffffffffffffffffffffffffffff")) {
                    hashRanges[0] = "00000000000000000000000000000001";
                    hashRanges[1] = idHashPairs.get(i).getHashVal();
                } else {
                    BigInteger value = new BigInteger(idHashPairs.get(idHashPairs.size() - 1).getHashVal(), 16);
                    value = value.add(BigInteger.ONE);
                    StringBuilder start = new StringBuilder(value.toString(16));
                    // Now we need to zero pad it if you actually want the full 32 chars.
                    while (start.length() < 32) {
                        start.insert(0, "0");
                    }
                    hashRanges[0] = start.toString();
                    // wraparound
                    hashRanges[1] = idHashPairs.get(i).getHashVal();
                }
            }
            idHashRangePairs.put(idHashPairs.get(i).getId(), hashRanges);
        }
    }

    public static void main(String[] args) {
//        ConsistentHash testHash = new ConsistentHash();
//        TreeMap<String, String[]> serverHashRange = new TreeMap<String, String[]>();
//        ArrayList<IDHashPair> idHashPairs = new ArrayList<IDHashPair>();
//
//        IDHashPair test1 = new IDHashPair("server1", testHash.getMD5("test1"));
//        idHashPairs.add(test1);
//
//        IDHashPair test2 = new IDHashPair("server2", testHash.getMD5("test2"));
//        idHashPairs.add(test2);
//
//        IDHashPair test0 = new IDHashPair("server0", testHash.getMD5("test0"));
//        idHashPairs.add(test0);
//
//        IDHashPair test3 = new IDHashPair("server3", "ffffffffffffffffffffffffffffffff");
//        idHashPairs.add(test3);
//
//        testHash.storeHashRange(idHashPairs);
    }
}
