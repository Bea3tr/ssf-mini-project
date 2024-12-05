package ssf.budgetbliss.repositories;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import ssf.budgetbliss.models.User;
import static ssf.budgetbliss.models.Constants.*;

@Repository
@SuppressWarnings("null")
public class UserRepository {

    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());
    private SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");

    @Autowired @Qualifier("redis-0")
    private RedisTemplate<String, Object> template;

    public User dbToUser(HashOperations<String, String, String> hashOps, String userId) {
        logger.info("[Repo] Retrieving user information from database");
        Map<String, String> userDetails = hashOps.entries(userId);
        float in = 0f;
        float out = 0f;
        for(String hashkey : userDetails.keySet()) {
            if(hashkey.contains("in_"))
                in += Float.parseFloat(userDetails.get(hashkey));
            else if (hashkey.contains("out_"))
                out += Float.parseFloat(userDetails.get(hashkey));
        }
        User user = new User(userId, userDetails.get(PASSWORD), userDetails.get(DEF_CURR), Float.parseFloat(userDetails.get(BALANCE)),
                         in, out, userDetails.get(TRANSACTIONS).split(","));

        return user;
    }

    public JsonObject dbToJson(HashOperations<String, String, String> hashOps, String userId) {
        logger.info("[Repo] Retrieving user information from database");
        Map<String, String> userDetails = hashOps.entries(userId);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for(String hashKey : userDetails.keySet()) {
            builder.add(hashKey, userDetails.get(hashKey));
        }
        JsonObject details = builder.build();

        return Json.createObjectBuilder().add(userId, details).build();
    }

    // hset userId password password
    public void insertUser(String userId, String password) {
        logger.info("[Repo] Inserting new user: " + userId);

        HashOperations<String, String, Object> hashOps = template.opsForHash();
        Map<String, Object> values = new HashMap<>();
        values.put(PASSWORD, password);
        values.put(BALANCE, 0f);
        values.put(TRANSACTIONS, "[%s] Created %s".formatted(df.format(new Date()), userId));
        
        hashOps.putAll(userId, values);
    }

    public void insertUser(HashOperations<String, String, Object> hashOps, String userId, User user) {
        Map<String, Object> values = new HashMap<>();
        values.put(PASSWORD, user.getPassword());
        values.put(BALANCE, user.getBalance());
        values.put(TRANSACTIONS, arrayToString(user.getTransactions(), ","));

        hashOps.putAll(userId, values);
    }

    public void updateUser(HashOperations<String, String, Object> hashOps, String userId, JsonObject user) {
        Set<String> details = user.keySet();
        Map<String, Object> values = new HashMap<>();
        for(String key : details) {
            values.put(key, user.get(key));
        }
        hashOps.putAll(userId, values);
    }

    public boolean userExists(String userId) {
        return template.hasKey(userId);
    }

    public Optional<User> getUser(String userId, String password) {
        logger.info("[Repo] Retrieving user information: " + userId);
        HashOperations<String, String, String> hashOps = template.opsForHash();
        User user = new User();

        String correctPassword = hashOps.get(userId, PASSWORD);
        if(!correctPassword.equals(password)) {
            logger.info("[Repo] Wrong password");
            return Optional.empty();
        }

        logger.info("[Repo] Getting user: " + userId);
        user = dbToUser(hashOps, userId);

        return Optional.of(user);
    }

    public User getUserById(String userId) {
        HashOperations<String, String, String> hashOps = template.opsForHash();
        User user = dbToUser(hashOps, userId);
        return user;
    }

    public void updateBal(String userId, String fromCurr, String cashflow, String trans_type, float amt) {
        logger.info("[Repo] Updating balance");
        HashOperations<String, String, Object> hashOps = template.opsForHash();

        float balance = Float.parseFloat(hashOps.get(userId, BALANCE).toString());
        float catBal = Float.parseFloat(hashOps.get(userId, cashflow + "_" + trans_type).toString());
        String transactions = hashOps.get(userId, TRANSACTIONS).toString();

        if(cashflow.equals("IN"))
            balance += amt;
        else 
            balance -= amt;

        catBal += amt;
        transactions += ",[%s] [%s] %s: $0.2f".formatted(df.format(new Date()), cashflow, trans_type, amt);
        hashOps.put(userId, BALANCE, balance);
        hashOps.put(userId, cashflow + "_" + trans_type, catBal);
        hashOps.put(userId, TRANSACTIONS, transactions);
    }
    
    public String arrayToString(String[] arr, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if(i != arr.length-1) {
                sb.append(arr[i]).append(delimiter);
            } else {
                sb.append(arr[i]);
            }
        }
        return sb.substring(0, sb.length()-1);
    }

    public void changeUserId(String userId, String newId) {
        JsonObject user = dbToJson(template.opsForHash(), userId);
        template.delete(userId);
        updateUser(template.opsForHash(), newId, user);
    }

    public boolean changePassword(String userId, String password, String newPassword) {
        HashOperations<String, String, String> hashOps = template.opsForHash();
        String correctPassword = hashOps.get(userId, PASSWORD);
        if(!password.equals(correctPassword)) {
            logger.info("[Repo] Wrong password entered");
            return false;
        }
        hashOps.put(userId, PASSWORD, newPassword);
        logger.info("[Repo] Password changed successfully");
        return true;
    }
}
