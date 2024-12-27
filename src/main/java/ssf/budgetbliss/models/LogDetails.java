package ssf.budgetbliss.models;

public class LogDetails {
 
    private String logName;
    private float balance;
    private float in;
    private float out;
    private String trendUrl;
    private String categoryUrl;

    public String getLogName() {return logName;}
    public void setLogName(String logName) {this.logName = logName;}

    public float getBalance() {return balance;}
    public void setBalance(float balance) {this.balance = balance;}

    public float getIn() {return in;}
    public void setIn(float in) {this.in = in;}

    public float getOut() {return out;}
    public void setOut(float out) {this.out = out;}

    public String getTrendUrl() {return trendUrl;}
    public void setTrendUrl(String trendUrl) {this.trendUrl = trendUrl;}

    public String getCategoryUrl() {return categoryUrl;}
    public void setCategoryUrl(String categoryUrl) {this.categoryUrl = categoryUrl;}

    public LogDetails(String logName, float balance, float in, float out, String trendUrl, String categoryUrl) {
        this.logName = logName;
        this.balance = balance;
        this.in = in;
        this.out = out;
        this.trendUrl = trendUrl;
        this.categoryUrl = categoryUrl;
    }

    
}
