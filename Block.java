import java.util.ArrayList;
import java.util.Date;

public class Block {
    public String hash;
    public String previousHash;
    /*public String data; //s simple message.*/
    public long timeStamp; //as number of milliseconds since 1/1/1970.
    public int nonce;
    public String merkleRoot;
    public ArrayList<Transaction> transactions = new ArrayList<>();
    //block constructor
    Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();//一定要在别的值都初始化之后再执行
    }
    // 计算哈希(我理解为一个标识 fingerprint，标记一个特定的区块)
    public String calculateHash() {
        String calculatedHash = StringUtil.applySha256(
                previousHash +
                        Long.toString(timeStamp) +
                        Integer.toString(nonce) +
                        merkleRoot

        );
        return calculatedHash;
    }
    public void mineBlock(int difficulty) {
        //创建一个有difficulty那么多的0的字符串，必须要hash也要这么多个0开头才挖矿成功(i.e可以创建新的区块)
        merkleRoot = StringUtil.getMerkleRoot(transactions);//先设置完这个
        String target = new String(new char[difficulty]).replace('\0','0');
        while(!hash.substring(0, difficulty).equals(target)) {
            //因为这个变量会影响计算出来的哈希
            ++nonce;
            hash = calculateHash();
        }
        System.out.println("Block mined!!!: " + hash);
    }
    public boolean addTransactions(Transaction transaction) {
        // process transaction and check if valid,
        // unless block is genesis block then ignore.
        if(transaction == null) {
            return false;
        }
        //排除 genesis Block
        if(previousHash != "0") {
            if(!transaction.processTransaction()) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }
        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }
}
