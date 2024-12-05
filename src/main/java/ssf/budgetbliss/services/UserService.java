package ssf.budgetbliss.services;

import java.util.Optional;
// import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ssf.budgetbliss.models.User;
import ssf.budgetbliss.repositories.UserRepository;

@Service
public class UserService {

    // private static final Logger logger = Logger.getLogger(UserService.class.getName());

    @Autowired
    private UserRepository userRepo;

    public void insertUser(String userId, String password) {
        userRepo.insertUser(userId, password);
    }

    public Optional<User> getUser(String userId, String password) {
        return userRepo.getUser(userId, password);
    }

    public User getUserById(String userId) {
        return userRepo.getUserById(userId);
    }

    public boolean userExists(String userId) {
        return userRepo.userExists(userId);
    }

    public void updateBal(String userId, String fromCurr, String cashflow, String trans_type, float amt) {
        userRepo.updateBal(userId, fromCurr, cashflow, trans_type, amt);
    }

    public void changeUserId(String userId, String newId) {
        userRepo.changeUserId(userId, newId);
    }

    public void changePassword(String userId, String password, String newPassword) {
        userRepo.changePassword(userId, password, newPassword);
    }
    
}
