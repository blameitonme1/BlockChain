import java.security.PublicKey;

public class TransactionOutput {
    public String id;
    public PublicKey recipient;
    public float value;
    public String parentTransactionId;//记录创建这个output的transaction的id
    //constructor
    TransactionOutput(PublicKey recipient, float value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtil.applySha256(StringUtil.getStringFromKey(recipient) +
                Float.toString(value) + parentTransactionId);
    }
    //检测这笔coin是否属于你
    boolean isMine(PublicKey publicKey) {
        return publicKey == recipient;
    }
}
