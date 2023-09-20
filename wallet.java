import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;
    //只储存所有属于自己的
    public Hashtable<String , TransactionOutput> UTXOs = new Hashtable<>();
    public wallet() {
        generateKeyPair();
    }
    public void generateKeyPair() {
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(ecSpec, random);   //256 bytes provides an acceptable security level
            KeyPair keyPair = keyGen.generateKeyPair();
            //设置keypair
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public float getBalance() {
        float total = 0;
        for(Map.Entry<String, TransactionOutput> item : Chain.UTXOs.entrySet()) {
            //遍历所有的UTXO，找到属于自己这个钱包的UTXO
            TransactionOutput UTXO = item.getValue();
            if(UTXO.isMine(publicKey)) {
                UTXOs.put(UTXO.id, UTXO);
                total += UTXO.value;
            }
        }
        return total;
    }
    // 从钱包产生新的交易
    public Transaction sendFunds(PublicKey _recipient, float value) {
        if(getBalance() < value) {
            //检查余额是否足够
            System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }
        // create lists of inputs;
        float total = 0;
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        for(Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            //有id就行了，之后可以靠id找
            inputs.add(new TransactionInput(UTXO.id));
            if(total > value) {
                break;
            }
        }
        Transaction newTransaction = new Transaction(publicKey, _recipient, value, inputs);
        //记得签名，利用私钥
        newTransaction.generateSignature(privateKey);
        //移除已经花费的
        for(TransactionInput input : inputs) {
            //这里的UTXOs是当前钱包的，不是区块链全局的UTXOs
            UTXOs.remove(input.transactionOutputId);
        }
        return newTransaction;
    }
}
