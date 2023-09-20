import java.security.*;
import java.util.ArrayList;
import java.util.Base64;

public class StringUtil {
    // 对于一个string使用Sha256加密算法并返回string ，只要知道这个就好了
    public static String applySha256(String input) {
        try {
            //选择了项目说的加密算法
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //将该算法用于输入
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();//转为16进制字符串
            for (int i = 0; i < hash.length ; i++) {
                String hex = Integer.toHexString(0xFF & hash[i]);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    //Applies ECDSA Signature and returns the result ( as bytes ).
    // 只需要知道applyECDSASig takes in the senders private key and string input,
    // signs it and returns an array of bytes.
    public static byte[] applyECDSAig(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }
    //verifyECDSASig takes in the signature, public key and string data and
    // returns true or false if the signature is valid.
    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    // getStringFromKey returns encoded string from any key.
    public static String  getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    //下面这个方法处理太多transaction的情况，使用了merkleTRee，以后有机会看看
    public static String getMerkleRoot(ArrayList<Transaction> transactions) {
        //Tacks in array of transactions and returns a merkle root.
        int count = transactions.size();
        ArrayList<String> previousTreeLayer = new ArrayList<String>();
        for(Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.transactionID);
        }
        ArrayList<String> treeLayer = previousTreeLayer;
        while(count > 1) {
            treeLayer = new ArrayList<String>();
            for(int i=1; i < previousTreeLayer.size(); i++) {
                treeLayer.add(applySha256(previousTreeLayer.get(i-1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }
        String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
        return merkleRoot;
    }
}
