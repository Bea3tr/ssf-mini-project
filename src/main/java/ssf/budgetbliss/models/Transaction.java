package ssf.budgetbliss.models;

import java.util.LinkedList;
import java.util.List;

public class Transaction {

    // [DATE] [CASHFLOW] TRANSTYPE: CURR AMT
    private String date;
    private String cashflow;
    private String transType;
    private String curr;
    private float amt;

    public String getDate() {return date;}
    public void setDate(String date) {this.date = date;}

    public String getCashflow() {return cashflow;}
    public void setCashflow(String cashflow) {this.cashflow = cashflow;}

    public String getTransType() {return transType;}
    public void setTransType(String transType) {this.transType = transType;}

    public String getCurr() {return curr;}
    public void setCurr(String curr) {this.curr = curr;}

    public float getAmt() {return amt;}
    public void setAmt(float amt) {this.amt = amt;}

    public Transaction(String date, String cashflow, String transType, String curr, float amt) {
        this.date = date;
        this.cashflow = cashflow;
        this.transType = transType;
        this.curr = curr;
        this.amt = amt;
    }

    @Override
    public String toString() {
        return "[%s] [%s] %s: %s %.2f".formatted(date, cashflow, transType, curr, amt);
    }
    
    public static Transaction stringToTransaction(String transaction) {
        String[] transDetails = transaction.trim()
                                .replaceAll("\\[", "")
                                .replaceAll("\\]", "")
                                .replaceAll("\\:", "")
                                .split(" ");
        return new Transaction(transDetails[0], transDetails[1], transDetails[2], 
            transDetails[3], Float.parseFloat(transDetails[4]));
    }

    public static List<Transaction> stringToTransactions(List<String> transactions) {
        List<Transaction> result = new LinkedList<>();
        for (String transaction : transactions) {
            if(transaction.contains(":")) {
                result.add(stringToTransaction(transaction));
            }
        }
        return result;
    }
}
