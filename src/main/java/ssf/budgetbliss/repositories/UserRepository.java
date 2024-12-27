package ssf.budgetbliss.repositories;

import java.io.StringReader;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import ssf.budgetbliss.models.Transaction;
import ssf.budgetbliss.models.User;

import static ssf.budgetbliss.models.Constants.*;

@Repository
@SuppressWarnings("null")
public class UserRepository {

    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());

    @Value("${curr.apikey}")
    private String CURR_APIKEY;

    @Autowired @Qualifier("redis-obj")
    private RedisTemplate<String, Object> templateObj;

    @Autowired @Qualifier("redis-string")
    private RedisTemplate<String, String> template;

    public List<String> getAllUserLogs(String userId) {
        return template.keys(userId + "*")
            .stream()
            .filter(key -> !key.contains("transactions"))
            .filter(key -> !key.contains("yearList"))
            .toList();
    }
    public JsonObject dbToJson(String userId) {
        HashOperations<String, String, Object> hashOps = template.opsForHash();
        logger.info("[Repo] Retrieving user information from database - to Json");
        Map<String, Object> userDetails = hashOps.entries(userId);
        JsonObjectBuilder userObj = Json.createObjectBuilder();
        for(String key : userDetails.keySet()) {
            userObj.add(key, userDetails.get(key).toString());
        }
        return userObj.build();
    }

    // hset userId password password
    public void insertUser(String userId, String password, String defCurr) {
        logger.info("[Repo] Inserting new user: " + userId);

        HashOperations<String, String, Object> hashOps = template.opsForHash();
        ListOperations<String, String> listOps = template.opsForList();
        Map<String, Object> values = new HashMap<>();
        values.put(PASSWORD, password);
        values.put(BALANCE, 0f);
        values.put(DEF_CURR, defCurr);
        hashOps.putAll(userId, values);
        listOps.leftPush(TRANSACTION_ID(userId), "[%s] Created %s".formatted(DF.format(new Date()), userId));
    }

    public boolean userExists(String userId) {
        return template.hasKey(userId);
    }

    public Optional<User> getUser(String userId, String password) {
        logger.info("[Repo] Retrieving user information: " + userId);
        HashOperations<String, String, Object> hashOps = template.opsForHash();
        User user = new User();

        String correctPassword = hashOps.get(userId, PASSWORD).toString();
        if(!correctPassword.equals(password)) {
            logger.info("[Repo] Wrong password");
            return Optional.empty();
        }

        logger.info("[Repo] Getting user: " + userId);
        user = dbToUser(hashOps, template.opsForList(), userId);

        return Optional.of(user);
    }

    public User getUserById(String userId) {
        User user = dbToUser(template.opsForHash(), template.opsForList(), userId);
        return user;
    }

    public void updateBal(String userId, String fromCurr, String cashflow, String trans_type, float amt, Date date, boolean isEdit) {
        logger.info("[Repo] Updating balance");
        HashOperations<String, String, Object> hashOps = template.opsForHash();
        String toCurr = hashOps.get(userId, DEF_CURR).toString();
        if(!fromCurr.equals(toCurr)) {
            logger.info("[Repo] Updating currency from %s to %s".formatted(fromCurr, toCurr));
            float conversion = convertCurrency(fromCurr, toCurr);
            amt *= conversion;
        }
        float balance = Float.parseFloat(hashOps.get(userId, BALANCE).toString());
        float catBal = 0f;
        if(hashOps.hasKey(userId, cashflow.toLowerCase() + "_" + trans_type.toLowerCase())) {
            catBal = Float.parseFloat(hashOps.get(userId, cashflow.toLowerCase() + "_" + trans_type.toLowerCase()).toString());
        } 
        if(cashflow.toLowerCase().equals(IN))
            balance += amt;
        else 
            balance -= amt;

        catBal += amt;
        hashOps.put(userId, BALANCE, ROUND_AMT(balance));
        hashOps.put(userId, cashflow.toLowerCase() + "_" + trans_type.toLowerCase(), ROUND_AMT(catBal));
        String year = DF.format(date).split("-")[0];
        if(!template.opsForList().range(YEARLIST(userId), 0, -1).contains(year)) {
            template.opsForList().leftPush(YEARLIST(userId), year);
        }
        if(!isEdit)
            template.opsForList().leftPush(TRANSACTION_ID(userId), createTransaction(cashflow, toCurr, trans_type, amt, date));
    }

    public void changeUserId(String userId, String newId) { 
        User user = dbToUser(template.opsForHash(), template.opsForList(), userId);
        template.delete(userId);
        updateUser(template.opsForHash(), template.opsForList(), newId, user);
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

    public void deleteUser(String userId) {
        logger.info("[Repo] Deleting user: " + userId);
        template.delete(template.keys(userId + "*"));
    }

    public Set<String> getCurrency() {
        if(template.hasKey("currencyList")) {
            logger.info("[Repo] Retrieving currency list from database");
            return Stream.of(template.opsForValue().get("currencyList")
            .toString()
            .trim()
            .split(","))
            .collect(Collectors.toSet());
        }
        logger.info("[Repo] Retrieving currency list from api");
        return new HashSet<>();
    }

    public void insertCurrencies(Set<String> currList) {
        String currencies = currList.toString()
            .replaceAll("\\[", "")
            .replaceAll("\\]", "")
            .replaceAll(" ", "");
        template.opsForValue().set("currencyList", currencies);
        template.expire("currencyList", 30, TimeUnit.DAYS);
    }

    public void editTransaction(String userId, int index, String edited) {
        updateDeletedTransactions(userId, template.opsForList().index(TRANSACTION_ID(userId), index));
        Transaction editedTrans = Transaction.stringToTransaction(edited);
        try {
            updateBal(userId, editedTrans.getCurr(), editedTrans.getCashflow(), editedTrans.getTransType(), 
            editedTrans.getAmt(), DF.parse(editedTrans.getDate()), true);
            logger.info("[Repo - edit] Balance updated");
        } catch (ParseException ex) {
            logger.warning("[Repo - edit] Error parsing date. Balance not updated");
        }
        template.opsForList().set(TRANSACTION_ID(userId), index, edited);
    }

    public void deleteTransaction(String userId, String transaction) {
        ListOperations<String, String> listOps = template.opsForList();
        logger.info("[Repo] Transaction deleted: " + transaction);
        updateDeletedTransactions(userId, transaction);
        listOps.remove(TRANSACTION_ID(userId), 1L, transaction);
    }

    public List<String> getTransactions(String transId) {
        return template.opsForList().range(transId, 0, -1);
    }

    public List<String> getFilteredTransactions(String transId, int year, int month) {
        if(month == 0) {
            return template.opsForList().range(transId, 0, -1)
                .stream()
                .filter(trans -> trans.contains(year + "-"))
                .toList();
        }
        return template.opsForList().range(transId, 0, -1)
                .stream()
                .filter(trans -> trans.contains(year + "-" + String.format("%02d", month)))
                .toList();
    }

    public void insertUserTrip(String userId, String name, String curr) {
        HashOperations<String, String, Object> hashOps = template.opsForHash();
        ListOperations<String, String> listOps = template.opsForList();
        hashOps.put(TRAVEL_ID(userId, name), DEF_CURR, curr);
        hashOps.put(TRAVEL_ID(userId, name), BALANCE, 0f);
        listOps.leftPush(TRANSACTION_ID(TRAVEL_ID(userId, name)), "[%s] Created %s".formatted(DF.format(new Date()), userId));
    }

    public String createTransaction(String cashflow, String curr, String trans_type, float amt, Date date) {
        return "[%s] [%s] %s: %s %.2f".formatted(DF.format(date), cashflow.toUpperCase(), 
            trans_type.toUpperCase(), curr, amt);
    }

    public void updateCurr(String userId, String toCurr) {
        HashOperations<String, String, Object> hashOps = template.opsForHash();
        if(!hashOps.get(userId, DEF_CURR).toString().equals(toCurr)) {
            float conversion = convertCurrency(hashOps.get(userId, DEF_CURR).toString(), toCurr);
            hashOps.put(userId, BALANCE, ROUND_AMT(Float.parseFloat(hashOps.get(userId, BALANCE).toString()) * conversion));
            for(String in : template.keys("in_")) {
                hashOps.put(userId, in, ROUND_AMT(Float.parseFloat(hashOps.get(userId, in).toString()) * conversion));
            }
            for(String out : template.keys("out_")) {
                hashOps.put(userId, out, ROUND_AMT(Float.parseFloat(hashOps.get(userId, out).toString()) * conversion));
            }
            template.opsForList().leftPush(TRANSACTION_ID(userId), "[%s] Converted currency from %s to %s".formatted(DF.format(new Date()), hashOps.get(userId, DEF_CURR), toCurr));
        }
    }

    public List<String> getTravelLogs(String userId) {
        logger.info("[Repo] UserId: " + userId);
        return template.keys(userId + "_*")
            .stream()
            .filter(key -> !key.contains("transactions"))
            .filter(key -> !key.contains("yearList"))
            .toList();
    }

    public List<Integer> getYears(String id) {
        List<Integer> years = new LinkedList<>();
        for(String year : template.opsForList().range(YEARLIST(id), 0, -1))
            years.add(Integer.parseInt(year));
        return years;
    }

    /* Private Methods */
    private User dbToUser(HashOperations<String, String, Object> hashOps, ListOperations<String, String> listOps, String userId) {
        logger.info("[Repo] Retrieving user information from database");
        Map<String, Object> userDetails = hashOps.entries(userId);
        User user = new User();
        float in = 0f;
        float out = 0f;
        for(String hashkey : userDetails.keySet()) {
            if(hashkey.contains("in_"))
                in += Float.parseFloat(userDetails.get(hashkey).toString());
            else if (hashkey.contains("out_"))
                out += Float.parseFloat(userDetails.get(hashkey).toString());
        }
        if(!userId.contains("_")) {
            user = new User(userId, userDetails.get(PASSWORD).toString(), userDetails.get(DEF_CURR).toString(), Float.parseFloat(userDetails.get(BALANCE).toString()),
            in, out, listOps.range(TRANSACTION_ID(userId), 0, -1));
        } else {
            user = new User(userId, userDetails.get(DEF_CURR).toString(), Float.parseFloat(userDetails.get(BALANCE).toString()),
            in, out, listOps.range(TRANSACTION_ID(userId), 0, -1));
        }
        return user;
    }

    private float convertCurrency(String from, String to) {
        String url = UriComponentsBuilder.fromUriString(CONVERT_URL)
            .queryParam("apikey", CURR_APIKEY)
            .queryParam("currencies", to)
            .queryParam("base_currency", from)
            .toUriString();
        
        RequestEntity<Void> req = RequestEntity.get(url)
            .build();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
        String payload = resp.getBody();

        JsonReader reader = Json.createReader(new StringReader(payload));
        logger.info("[Repo] Payload from currency conversion: " + payload);
        float conversion = Float.parseFloat(reader.readObject()
            .getJsonObject("data")
            .getJsonNumber(to)
            .toString());
        
        return conversion;
    }

    private void updateUser(HashOperations<String, String, Object> hashOps, ListOperations<String, String> listOps, String userId, User user) {
        Map<String, Object> values = new HashMap<>();
        values.put(USERID, user.getUserId());
        values.put(PASSWORD, user.getPassword());
        values.put(DEF_CURR, user.getDefCurr());
        values.put(BALANCE, user.getBalance());
        values.put(IN, user.getIn());
        values.put(OUT, user.getOut());
        hashOps.putAll(userId, values);
        for (String trans : user.getTransactions()) {
            listOps.rightPush(TRANSACTION_ID(userId), trans);
        }
    }

    private void updateDeletedTransactions(String userId, String transaction) {
        logger.info("[Repo] Transaction to delete: " + transaction);
        if(transaction.contains(":")) {
            Transaction trans = Transaction.stringToTransaction(transaction);
            HashOperations<String, String, Object> hashOps = template.opsForHash();
            String hashKey = trans.getCashflow().toLowerCase()+"_"+trans.getTransType().toLowerCase();
            float ogAmt = Float.parseFloat(hashOps.get(userId, hashKey).toString());
            if(!trans.getCurr().equals(hashOps.get(userId, DEF_CURR)))
                ogAmt *= convertCurrency(trans.getCurr(), hashOps.get(userId, DEF_CURR).toString());
            // Update cashflow category 
            hashOps.put(userId, hashKey, ROUND_AMT(ogAmt - trans.getAmt()));
            logger.info("[Repo] Updated %s from %.2f to %.2f".formatted(hashKey, ogAmt, ogAmt-trans.getAmt()));
            // Update balance
            logger.info("[Repo] Cashflow: " + trans.getCashflow());
            if(trans.getCashflow().equals("IN"))
                hashOps.put(userId, BALANCE, ROUND_AMT(Float.parseFloat(hashOps.get(userId, BALANCE).toString()) - trans.getAmt()));
            else 
                hashOps.put(userId, BALANCE, ROUND_AMT(Float.parseFloat(hashOps.get(userId, BALANCE).toString()) + trans.getAmt()));
        }
    }
}
