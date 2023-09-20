import com.google.gson.GsonBuilder;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

public class Chain {
    //储存所有的block
    public static ArrayList<Block> blockchain  = new ArrayList<>();
    //所有没有被花费的交易
    public static Hashtable<String, TransactionOutput> UTXOs = new Hashtable<>();
    public static wallet walletA;
    public static wallet walletB;
    public static int difficulty = 6;//设置挖矿难度
    public static float minimumTransaction = 0.1f;
    public static Transaction genesisTransaction;
    public static void main(String[] args) {
        //Setup Bouncey castle as a Security Provider
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        //Create the new wallets
        walletA = new wallet();
        walletB = new wallet();
        wallet coinbase = new wallet();
        //create genesis transaction, which sends 100 NoobCoin to walletA:
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction
        genesisTransaction.transactionID = "0"; //manually set the transaction id
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionID)); //manually add the Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.

        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.addTransactions(genesisTransaction);
        addBlock(genesis);

        //testing
        Block block1 = new Block(genesis.hash);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("\nWalletA is Attempting to send funds (40) to WalletB...");
        block1.addTransactions(walletA.sendFunds(walletB.publicKey, 40f));
        addBlock(block1);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block2 = new Block(block1.hash);
        System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
        block2.addTransactions(walletA.sendFunds(walletB.publicKey, 1000f));
        addBlock(block2);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block3 = new Block(block2.hash);
        System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
        block3.addTransactions(walletB.sendFunds( walletA.publicKey, 20));
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        isChainValid();
        /*//将区块链的java object转化为json文件
        String blockchainJson  = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
        System.out.println("\n The block chain : ");
        System.out.println(blockchainJson);*/
    }
    public static void addBlock(Block newBlock) {
        //先挖矿再说
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
    //对任何block改变都会导致这个方法无效
    public static boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0','0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        for (int i = 1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);
            //比较当前储存的前一个哈希的和前一个的哈希是否一样
            if(!currentBlock.previousHash.equals(previousBlock.hash)) {
                System.out.println("previous hashes not equal");
                return false;
            }
            //比较计算的哈希和当前的哈希
            if(!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("Current Hashes not equal");
                return false;
            }
            //检查是否当前哈希是被解决了mine的，也就是是否满足mineBlock的标准
            if(!currentBlock.hash.substring(0,difficulty).equals(hashTarget)) {
                System.out.println("The block isn't solved.");
                return false;
            }
            // 遍历当前block的所有交易
            TransactionOutput tempOutput;
            for (int j = 0; j < currentBlock.transactions.size(); j++) {
                Transaction tempTransaction = currentBlock.transactions.get(j);
                if(!tempTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + j + ") is Invalid");
                    return false;
                }
                //检测之处是否平衡(支出会影响交易双方)
                if(tempTransaction.getInputsValue() != tempTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + j+ ")");
                    return false;
                }
                for(TransactionInput input : tempTransaction.inputs) {
                    // 从当前所有没花费的output里面寻找
                    tempOutput = tempUTXOs.get(input.transactionOutputId);
                    if(tempOutput == null) {
                        // 没有一个对应的支出，凡是交易必定会产生支出
                        System.out.println("#Referenced input on Transaction(" + j + ") is Missing");
                        return false;
                    }
                    if(input.UTXO.value != tempOutput.value) {
                        // 对应的金额不同
                        System.out.println("#Referenced input Transaction(" + j + ") value is Invalid");
                        return false;
                    }
                    //表示input已经被花费了，因为input对应的output是之前交易产生的，也就是input里面的output是余额
                    tempUTXOs.remove(input.transactionOutputId);
                }
                // 加入当前所有未花费货币
                for(TransactionOutput output:tempTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }
                //因为会生成两笔output，一个给sender，一个给recipients
                if( tempTransaction.outputs.get(0).recipient != tempTransaction.recipient) {
                    System.out.println("#Transaction(" + j + ") output reciepient is not who it should be");
                    return false;
                }
                if( tempTransaction.outputs.get(1).recipient != tempTransaction.sender) {

                    System.out.println("#Transaction(" + j + ") output 'change' is not sender.");
                    return false;
                }
            }
        }
        System.out.println("Blockchain is valid");
        return true;
    }
}
