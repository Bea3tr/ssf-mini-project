package ssf.budgetbliss.models;

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
    
}
