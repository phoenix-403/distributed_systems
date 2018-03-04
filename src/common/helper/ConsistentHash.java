package common.helper;
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
            System.out.println("number: " + number);
            StringBuilder hashText = new StringBuilder(number.toString(16));
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            System.out.println("string: " + hashText.toString());
            return hashText.toString();
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException e){
            throw new RuntimeException(e);
        }
    }


    private static class IdHashPair implements Comparable<IdHashPair> {
        private String id;
        private String hashVal;
        public IdHashPair(String id, String hashVal) {
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
        public int compareTo(IdHashPair o) {
            if (this.getHashVal().compareTo(o.getHashVal()) < 0) {
                return -1;
            } else if (this.getHashVal().compareTo(o.getHashVal()) > 0) {
                return 1;
            }
            return 0;
        }
    }

    private static IdHashPair getServerResponsible(IdHashPair tupleHashed, ArrayList<IdHashPair> idHashPairs){
        int index = 0;
        for(int i = 0; i < idHashPairs.size(); i++){
            if(tupleHashed.compareTo(idHashPairs.get(i)) < 0)
                index = i%idHashPairs.size();
        }
        return idHashPairs.get(index);
    }

    public static void main(String[] args) {
        ConsistentHash testHash = new ConsistentHash();
        ArrayList<IdHashPair> idHashPairs = new ArrayList<IdHashPair>();

//        System.out.println(testHash.getMD5("testing shit"));
        IdHashPair test1 = new IdHashPair("server1", testHash.getMD5("test1"));
        idHashPairs.add(test1);
        IdHashPair test2 = new IdHashPair("server2", testHash.getMD5("test2"));
        idHashPairs.add(test2);
        IdHashPair test0 = new IdHashPair("server0", testHash.getMD5("test0"));
        idHashPairs.add(test0);
//        IdHashPair tuple0 = new IdHashPair("tuple0", testHash.getMD5("test3"));
//        idHashPairs.add(tuple0);
        IdHashPair tuple0 = new IdHashPair("tuple0", "ffffffffffffffffffffffffffffffff");
        idHashPairs.add(test0);
        Collections.sort(idHashPairs);
        IdHashPair serverResponsible = getServerResponsible(tuple0, idHashPairs);
    }
}
