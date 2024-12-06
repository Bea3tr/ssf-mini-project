package ssf.budgetbliss.models;

import jakarta.validation.constraints.*;

public class ValidUser {

    @NotNull(message="User ID cannot be null")
    @NotEmpty(message="User ID cannot be empty")
    private String userId;

    @NotNull(message="Password cannot be null")
    @NotEmpty(message="Password cannot be empty")
    private String password;
    private String confirmPassword;

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}
    
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    
    public String getConfirmPassword() {return confirmPassword;}
    public void setConfirmPassword(String confirmPassword) {this.confirmPassword = confirmPassword;}
    
}
