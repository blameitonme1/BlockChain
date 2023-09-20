public class TransactionInput {
    /*This class will be used to reference TransactionOutputs that
     have not yet been spent.
     The transactionOutputId will be used to find the relevant TransactionOutput,
     allowing miners to check your ownership.*/

    // Reference to TransactionOutputs -> transactionId
    public String transactionOutputId;
    public TransactionOutput UTXO; //Contains the Unspent transaction output,也就是收入的交易
    public TransactionInput(String transactionOutput) {
        this.transactionOutputId = transactionOutput;
    }
}
