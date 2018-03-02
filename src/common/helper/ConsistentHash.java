package common.helper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.math.*;

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

    public void addNode(String hashedVal){

    }

    public static void main(String[] args) throws IOException {
        ConsistentHash testHash = new ConsistentHash();
        System.out.println(testHash.getMD5("testing shit"));
    }
}
