package ssf.budgetbliss.models;

public class ValidUser {

    private String userId;
    private String password;
    private String confirmPassword;

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}
    
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    
    public String getConfirmPassword() {return confirmPassword;}
    public void setConfirmPassword(String confirmPassword) {this.confirmPassword = confirmPassword;}
    
}
