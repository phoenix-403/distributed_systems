package common.helper;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

public class ConsistentHash {
    public String getMD5(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(key.getBytes("UTF-8"));
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


    public static IDHashPair getServerResponsible(IDHashPair tupleHashed, ArrayList<IDHashPair> idHashPairs){
        int index = 0;
        for(int i = 0; i < idHashPairs.size(); i++){
            if(tupleHashed.compareTo(idHashPairs.get(i)) < 0)
                index = i%idHashPairs.size();
        }
        return idHashPairs.get(index);
    }

    public static void main(String[] args) {
        ConsistentHash testHash = new ConsistentHash();
        ArrayList<IDHashPair> idHashPairs = new ArrayList<IDHashPair>();

//        System.out.println(testHash.getMD5("testing shit"));
        IDHashPair test1 = new IDHashPair("server1", testHash.getMD5("test1"));
        idHashPairs.add(test1);
        IDHashPair test2 = new IDHashPair("server2", testHash.getMD5("test2"));
        idHashPairs.add(test2);
        IDHashPair test0 = new IDHashPair("server0", testHash.getMD5("test0"));
        idHashPairs.add(test0);
//        IDHashPair tuple0 = new IDHashPair("tuple0", testHash.getMD5("test3"));
//        idHashPairs.add(tuple0);
        IDHashPair tuple0 = new IDHashPair("tuple0", "f1ffffffffffffffffffffffffffffff");
        idHashPairs.add(tuple0);
        Collections.sort(idHashPairs);
        IDHashPair serverResponsible = getServerResponsible(tuple0, idHashPairs);
        idHashPairs.remove(test0);
        Collections.sort(idHashPairs);
        IDHashPair serverResponsible2 = getServerResponsible(tuple0, idHashPairs);
    }
}
