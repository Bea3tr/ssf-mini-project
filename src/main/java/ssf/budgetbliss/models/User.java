package ssf.budgetbliss.models;

import java.util.List;

public class User {

    private String userId;
    private String password;
    private String defCurr;

    private float balance;
    private float in;
    private float out;
    
    private List<String> transactions;

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}
    
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public String getDefCurr() {return defCurr;}
    public void setDefCurr(String defCurr) {this.defCurr = defCurr;} 

    public float getBalance() {return balance;}
    public void setBalance(float balance) {this.balance = balance;}

    public float getIn() {return in;}
    public void setIn(float in) {this.in = in;}
    
    public float getOut() {return out;}
    public void setOut(float out) {this.out = out;}
    
    public List<String> getTransactions() {return transactions;}
    public void setTransactions(List<String> transactions) {this.transactions = transactions;}
    
    public User() {}
    public User(String userId, String password, String defCurr, float balance, float in, float out,
            List<String> transactions) {
        this.userId = userId;
        this.password = password;
        this.defCurr = defCurr;
        this.balance = balance;
        this.in = in;
        this.out = out;
        this.transactions = transactions;
    }  
    
}
