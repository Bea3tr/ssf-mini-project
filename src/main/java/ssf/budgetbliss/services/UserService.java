package ssf.budgetbliss.services;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.Set;

import static ssf.budgetbliss.models.Constants.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonReader;
import jakarta.servlet.http.HttpSession;
import ssf.budgetbliss.models.User;
import ssf.budgetbliss.repositories.UserRepository;

@Service
public class UserService {

    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    @Value("${curr.apikey}")
    private String CURR_APIKEY;

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

    public boolean changePassword(String userId, String password, String newPassword) {
        return userRepo.changePassword(userId, password, newPassword);
    }

    public void deleteUser(String userId) {
        userRepo.deleteUser(userId);
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
            logger.info("[User Service] Unauthenticated access");
            return false;
        }
        logger.info("[User Service] Access authenticated");
        return true;
    }

    // IN vs OUT, daily cashflow over a month, type of transactions (IN & OUT)
    public List<String> getDefaultCharts(User user) {
        List<String> imgUrls = new LinkedList<>();
        
        // IN vs OUT
        Set<String> labels = new HashSet<>(){{
            add("IN");
            add("OUT");
        }};
        List<Float> data = new LinkedList<>() {{
            add(user.getIn());
            add(user.getOut());
        }};
        logger.info("IN: " + user.getIn() + " OUT: " + user.getOut());
        JsonObject param = chartObj("doughnut", labels, data, "IN vs OUT");
        logger.info("IN vs OUT: " + param.toString());
        imgUrls.add(getUrl(param));

        // Daily cashflow
        String[] transactions = user.getTransactions();
        Map<String, Float> cashMap = new HashMap<>();
        for(String trans : transactions) {
            if(!trans.contains(":"))
                continue;
            // [DATE] [CASHFLOW] TYPE CURR AMT
            String[] details = trans.trim().split(" ");
            String date = details[0];
            String flow = details[1];
            float amt = Float.parseFloat(details[4]);
            if(flow.equals("[OUT]"))
                amt *= -1;
            if(!cashMap.containsKey(date)) {
                cashMap.put(date, amt);
            } else {
                cashMap.put(date, cashMap.get(date) + amt);
            }
        }
        JsonObject param2 = chartObj("bar", cashMap.keySet(), 
            cashMap.values().stream().toList(), "Daily Cashflow");
        imgUrls.add(getUrl(param2));

        // All transaction type
        JsonObject userObj = userRepo.dbToJson(user.getUserId());
        Map<String, Float> transType = new HashMap<>();
        for(String key : userObj.keySet()) {
            if(key.contains("out_")) {
                transType.put(key, Float.parseFloat(userObj.getString(key)));
            }
        }
        JsonObject param3 = chartObj("doughnut", transType.keySet(), 
            transType.values().stream().toList(), "Transaction Categories");
        imgUrls.add(getUrl(param3));
        return imgUrls;
    }

    private String getUrl(JsonObject param) {
        return UriComponentsBuilder.fromUriString(CHART_URL)
                .queryParam("c", param.toString())
                .toUriString();
    }

    private JsonObject chartObj(String type, Set<String> labels, List<Float> data, String title) {
        JsonObject options = Json.createObjectBuilder()
            .add("title", Json.createObjectBuilder()
                .add("display", true)
                .add("text", title)
                .build())
            .build();

        JsonObject dataObj = Json.createObjectBuilder()
            .add("datasets", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("data", buildFloatArray(data))
                    .add("backgroundColor", buildStringArray(BG_COLOR))
                    .build())
                .build())
            .add("labels", buildStringArray(labels))
            .build();
        
        return Json.createObjectBuilder()
            .add("type", type)
            .add("data", dataObj)
            .add("options", options)
            .build(); 
    }

    private JsonArray buildStringArray(Set<String> values) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(String value : values)
            builder.add(value);
        return builder.build();
    }

    private JsonArray buildFloatArray(List<Float> values) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(Float value : values)
            builder.add(value);
        return builder.build();
    }

}
