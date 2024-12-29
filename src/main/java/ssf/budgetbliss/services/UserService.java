package ssf.budgetbliss.services;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.Set;
import java.util.TreeMap;

import static ssf.budgetbliss.models.Constants.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.javapoet.ClassName;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonReader;
import jakarta.servlet.http.HttpSession;
import ssf.budgetbliss.models.LogDetails;
import ssf.budgetbliss.models.Transaction;
import ssf.budgetbliss.models.User;
import ssf.budgetbliss.repositories.UserRepository;

@Service
public class UserService {

    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    
    @Value("${local.url}")
    private String LOCAL_URL;

    @Value("${curr.apikey}")
    private String CURR_APIKEY;

    @Value("${my.apikey}")
    private String MY_APIKEY;

    @Autowired
    private UserRepository userRepo;

    public void insertUser(String userId, String password, String defCurr) {
        userRepo.insertUser(userId, password, defCurr);
    }

    public void insertUserTrip(String userId, String name, String curr) {
        userRepo.insertUserTrip(userId, name, curr);
    }

    public Optional<User> getUser(String userId, String password) {
        return userRepo.getUser(userId, password);
    }

    public User getUserById(String userId) {
        return userRepo.getUserById(userId);
    }

    public List<String> getAllUserLogs(String userId) {
        return userRepo.getAllUserLogs(userId);
    }

    public boolean userExists(String userId) {
        return userRepo.userExists(userId);
    }

    public void updateBal(String userId, String fromCurr, String cashflow, String trans_type, float amt, Date date, boolean isEdit) {
        userRepo.updateBal(userId, fromCurr, cashflow, trans_type, amt, date, isEdit);
    }

    public void changeUserId(String userId, String newId) {
        userRepo.changeUserId(userId, newId);
    }

    public boolean changePassword(String userId, String password, String newPassword) {
        return userRepo.changePassword(userId, password, newPassword);
    }

    public void deleteUser(String userId) {
        userRepo.deleteUser(userId);
    }

    public List<String> getTransactions(String transId) {
        return userRepo.getTransactions(transId);
    }

    public List<String> getFilteredTransactions(String transId, int year, int month) {
        return userRepo.getFilteredTransactions(transId, year, month);
    }

    public void editTransaction(String userId, int index, String edited) {
        userRepo.editTransaction(userId, index, edited);
    }

    public void deleteTransaction(String userId, String transaction) {
        userRepo.deleteTransaction(userId, transaction);
    }

    public String createTransaction(String cashflow, String curr, String trans_type, float amt, Date date) {
        return userRepo.createTransaction(cashflow, curr, trans_type, amt, date);
    }

    public void updateCurr(String userId, String toCurr) {
        userRepo.updateCurr(userId, toCurr);
    }

    public List<String> getTravelLogs(String userId) {
        return userRepo.getTravelLogs(userId);
    }

    public String getDefCurr(String userId) {
        return userRepo.getDefCurr(userId);
    }

    public Set<String> currencyList() {
        Set<String> currList = userRepo.getCurrency();
        if(currList.size() > 0) {
            return currList;
        }
        String url = UriComponentsBuilder.fromUriString(CURR_URL)
                .queryParam("apikey", CURR_APIKEY)
                .toUriString();

        RequestEntity<Void> req = RequestEntity.get(url)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
        String payload = resp.getBody();

        JsonReader reader = Json.createReader(new StringReader(payload));
        JsonObject data = reader.readObject().getJsonObject("data");
        currList = data.keySet();
        userRepo.insertCurrencies(currList);

        return currList;
    }

    public boolean isAuth(HttpSession sess, String userId) {
        String id = (String) sess.getAttribute(USERID);
        if (id == null || !userId.equals(id)) {
            logger.info("[Service] Unauthenticated access");
            return false;
        }
        logger.info("[Service] Access authenticated");
        return true;
    }

    public List<Integer> getYearList(String id) {
        return userRepo.getYears(id);
    }

    private String getUrl(JsonObject param) {
        RequestEntity<String> req = RequestEntity.post(CHART_RENDER_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(param.toString());
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
        String payload = resp.getBody();
        return Json.createReader(new StringReader(payload))
            .readObject()
            .getString("url");
    }

    public JsonObject chartObj(String type, Set<String> labels, List<Float> data) {
        JsonObject dataObj = Json.createObjectBuilder()
            .add("datasets", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("label", "Amount")
                    .add("data", buildFloatArray(data))
                    .add("backgroundColor", buildStringArray(BG_COLOR))
                    .build())
                .build())
            .add("labels", buildStringArray(labels))
            .build();
        
        return Json.createObjectBuilder()
            .add("chart", Json.createObjectBuilder()
                .add("type", type)
                .add("data", dataObj)
                .build())
            .build(); 
    }

    public Map<String, String> getCharts(String id, String filter) {
        Map<String, String> imgList = new HashMap<>();
        RequestEntity<String> req = RequestEntity.post(LOCAL_URL + "/userdb")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Json.createObjectBuilder()
                .add("apikey", MY_APIKEY)
                .add("id", id)
                .build().toString());

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
        String payload = resp.getBody();
        JsonObject chartsByFilter = Json.createReader(new StringReader(payload))
            .readObject()
            .getJsonObject(id)
            .getJsonObject(filter);
        imgList.put("IN vs OUT", getUrl(chartsByFilter.getJsonObject("in vs out")));
        imgList.put("TRENDS", getUrl(chartsByFilter.getJsonObject("trends")));
        imgList.put("SPENDING CATEGORIES", getUrl(chartsByFilter.getJsonObject("spending categories")));
        return imgList;
    }

    private JsonArray buildStringArray(Set<String> values) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(String value : values)
            builder.add(value);
        return builder.build();
    }

    private JsonArray buildFloatArray(List<Float> values) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(Float value : values){
            // Pass as integer
            builder.add(Math.round(value));
        }
        return builder.build();
    }

    public float getSumOfTransactions(List<Transaction> transactions, String filter) {
        float sum = 0f;
        for(Transaction trans : transactions) {
            if(trans.getCashflow().toLowerCase().equals(filter))
                sum += trans.getAmt();
        }
        return sum;
    }

    public JsonObject getInOutChart(List<Transaction> transactions) {
        // IN vs OUT
        Set<String> labels = new HashSet<>(){{
            add("IN");
            add("OUT");
        }};
        List<Float> data = new LinkedList<>() {{
            add(getSumOfTransactions(transactions, IN));
            add(getSumOfTransactions(transactions, OUT));
        }};
        return chartObj("doughnut", labels, data);
    }

    public JsonObject getDailyTrend(List<Transaction> transactions) {
        // Daily cashflow
        Map<String, Float> cashMap = new TreeMap<>();
        for(Transaction trans : transactions) {
            // [DATE] [CASHFLOW] TYPE CURR AMT
            String date = trans.getDate();
            String flow = trans.getCashflow().toLowerCase();
            float amt = trans.getAmt();
            if(flow.equals(OUT))
                amt *= -1;
            if(!cashMap.containsKey(date)) {
                cashMap.put(date, amt);
            } else {
                cashMap.put(date, cashMap.get(date) + amt);
            }
        }
        return chartObj("bar", cashMap.keySet(), 
            cashMap.values().stream().toList());
    }

    public JsonObject getMonthlyTrend(List<Transaction> transactions) {
        // Monthly cashflow
        Map<String, Float> cashMap = new TreeMap<>();
        for(Transaction trans : transactions) {
            // [DATE] [CASHFLOW] TYPE CURR AMT
            String ym = trans.getDate().split("-")[0] + "-" + trans.getDate().split("-")[1];
            String flow = trans.getCashflow().toLowerCase();
            float amt = trans.getAmt();
            if(flow.equals(OUT))
                amt *= -1;
            if(!cashMap.containsKey(ym)) {
                cashMap.put(ym, amt);
            } else {
                cashMap.put(ym, cashMap.get(ym) + amt);
            }
        }
        logger.info("[Service] CashMap: " + cashMap);
        return chartObj("bar", cashMap.keySet(), 
            cashMap.values().stream().toList());
    }

    public JsonObject getAllCategories(List<Transaction> transactions) {
        // All transaction type
        Map<String, Float> transType = new HashMap<>();
        for(Transaction trans : transactions) {
            if(trans.getCashflow().toLowerCase().equals(OUT)) {
                if(!transType.containsKey(trans.getTransType()))
                    transType.put(trans.getTransType().toUpperCase(), trans.getAmt());
                else {
                    transType.put(trans.getTransType().toUpperCase(), 
                        transType.get(trans.getTransType().toUpperCase()) + trans.getAmt());
                }
            } 
        }
        return chartObj("doughnut", transType.keySet(), 
            transType.values().stream().toList());
    }

    public List<LogDetails> getLogDetails(String userId) {
        List<String> userLogs = userRepo.getAllUserLogs(userId);
        List<LogDetails> logDetails = new LinkedList<>();
        RequestEntity<String> req = RequestEntity.post(LOCAL_URL + "/userall")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Json.createObjectBuilder()
                .add("apikey", MY_APIKEY)
                .add("id", userId)
                .build().toString());
    
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
        String payload = resp.getBody();

        for (String id : userLogs) {
            User user = userRepo.getUserById(id);
        
            JsonObject chartsById = Json.createReader(new StringReader(payload))
                .readObject()
                .getJsonObject(userId)
                .getJsonObject(id);

            logDetails.add(new LogDetails(id, user.getDefCurr(), user.getBalance(), user.getIn(), user.getOut(), 
                getUrl(chartsById.getJsonObject("trends")), 
                getUrl(chartsById.getJsonObject("spending categories"))));
        }
        Collections.sort(logDetails, (a, b) -> a.getLogName().compareTo(b.getLogName()));
        logger.info("[Service] Order of log details: " + logDetails);
        return logDetails;
    }

    public float getConversion(String from, String to) {
        return userRepo.getConversion(from, to);
    }

    public ResponseEntity<String> checkHealth() {
		try {
            userRepo.checkHealth();
            return ResponseEntity.status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}");

        } catch (Exception ex) {
            return ResponseEntity.status(503)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}");
        }
	}
}
