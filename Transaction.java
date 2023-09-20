import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {
    public String transactionID;//同时也是交易的哈希
    public PublicKey sender;//发送者的地址
    public PublicKey recipient;//接收者的地址
    public float value;
    public byte[] signature;// this is to prevent anybody else from spending funds in our wallet.
    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
    public static int sequence = 0;//估计有多少交易产生了
    //constructor
    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput>inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }
    //接下来计算transaction hash，也会被当作id
    public String calculateHash() {
        sequence++; //防止两次同样的交易有相同id
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(recipient) +
                        Float.toString(value) +
                        sequence
        );
    }
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient)
                 + Float.toString(value);
        //使用公钥和私钥生成签名
        signature = StringUtil.applyECDSAig(privateKey, data);
    }
    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient)
                + Float.toString(value);
        // In reality, you may want to sign more information,
        // like the outputs/inputs used and/or time-stamp
        // ( for now we are just signing the bare minimum )
        // 现在只是将私钥和一些交易信息签名了（i.e 生成一系列相关的byte序列）
        return StringUtil.verifyECDSASig(sender , data , signature);
    }
    public boolean processTransaction() {
        if(!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }
        //gather transaction inputs (Make sure they are unspent):
        for (TransactionInput i : inputs) {
            //这一步是确保所有输入都是没有被花费过的
            i.UTXO = Chain.UTXOs.get(i.transactionOutputId);
        }
        //检查交易是否满足要求
        if(getInputsValue() < Chain.minimumTransaction) {
            System.out.println("#Transaction Inputs to small: " + getInputsValue());
            return false;
        }
        //产生一个TransactionOutput
        float leftOver = getInputsValue() - value;
        transactionID = calculateHash();
        //分别将两个交易结果加入到产生的outputs
        outputs.add(new TransactionOutput(recipient, value, transactionID));
        outputs.add(new TransactionOutput(sender, leftOver, transactionID));

        //交易是以TransactionOutputs的形式进行的
        for(TransactionOutput o : outputs) {
            Chain.UTXOs.put(o.id, o);
        }

        // 将所有的输入都从UTXO(也就是没花费过的output的集合)
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) {
                continue;
            }
            Chain.UTXOs.remove(i.UTXO.id);
        }
        return true;
    }
    //计算所有当前余额
    public float getInputsValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) {
                continue;
            }
            total += i.UTXO.value; // i.transactionId也可以
        }
        return total;
    }
    // 计算支出
    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }
}
