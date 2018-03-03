package common.helper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.math.*;
import java.util.*;

public class ConsistentHash {
    public String getMD5(String preImage) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(preImage.getBytes("UTF-8"));
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException e){
            throw new RuntimeException(e);
        }
    }


    private static class idHash_pair implements Comparable<idHash_pair> {
        private String id;
        private String hashVal;
        public idHash_pair(String id, String hashVal) {
            this.id = id;
            this.hashVal = hashVal;
        }

        public String getId() {
            return id;
        }

        public String getHashVal() {
            return hashVal;
        }

        @Override
        public int compareTo(idHash_pair o) {
            if (this.getHashVal().compareTo(o.getHashVal()) == -1) {
                return -1;
            } else if (this.getHashVal().compareTo(o.getHashVal()) == 1) {
                return 1;
            }
            return 0;
        }
    }

    public static void main(String[] args) {
        ConsistentHash testHash = new ConsistentHash();
        ArrayList<idHash_pair> idHashes = new ArrayList<idHash_pair>();

        System.out.println(testHash.getMD5("testing shit"));
        idHash_pair test1 = new idHash_pair("server1", testHash.getMD5("test1"));
        idHashes.add(test1);
        idHash_pair test2 = new idHash_pair("server2", testHash.getMD5("test2"));
        idHashes.add(test2);
        idHash_pair test0 = new idHash_pair("server0", testHash.getMD5("test0"));
        idHashes.add(test0);
        Collections.sort(idHashes);
        for(idHash_pair idhash : idHashes){
            System.out.println(idhash);
        }
    }
}
