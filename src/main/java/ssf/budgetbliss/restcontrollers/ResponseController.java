package ssf.budgetbliss.restcontrollers;

import static ssf.budgetbliss.models.Constants.MONTHS;
import static ssf.budgetbliss.models.Constants.TRANSACTION_ID;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import ssf.budgetbliss.models.Transaction;
import ssf.budgetbliss.services.UserService;

@RestController
@RequestMapping
public class ResponseController {

    @Value("${my.apikey}")
    private String apikey;

    @Autowired
    private UserService userSvc;
    
    @PostMapping(path="/userdb", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postUserDB(@RequestBody String payload) {
        JsonReader reader = Json.createReader(new StringReader(payload));
        JsonObject obj = reader.readObject();
        String id = obj.getString("id");
        Map<String, List<Transaction>> filteredTrans = new HashMap<>();
        JsonObjectBuilder respBuilder = Json.createObjectBuilder();

        // Filter by year-month, e.g. 2024-JAN
        for (int year : userSvc.getYearList(id)) {
            for (int month = 0; month < 13; month++) {
                filteredTrans.put(year+"-"+MONTHS[month], Transaction.stringToTransactions(
                    userSvc.getFilteredTransactions(TRANSACTION_ID(id), year, month)));
            }
        }

        JsonObjectBuilder chartBuilder = Json.createObjectBuilder();
        for(String logId : filteredTrans.keySet()) {
            JsonObject trends = userSvc.getDailyTrend(filteredTrans.get(logId));
            if(logId.split("-")[1].equals("ALL") && !id.contains("_"))
                trends = userSvc.getMonthlyTrend(filteredTrans.get(logId));
            // Get default charts
            chartBuilder.add(logId, Json.createObjectBuilder() 
                .add("in vs out", userSvc.getInOutChart(filteredTrans.get(logId)))
                .add("trends", trends)
                .add("spending categories", userSvc.getAllCategories(filteredTrans.get(logId)))
                .build());   
        }
        try {
            if(obj.getString("apikey").equals(apikey)) {
                return ResponseEntity.status(201)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body((respBuilder.add(id, chartBuilder.build()).build()).toString());
            } else {
                return ResponseEntity.status(400)
                    .body("{\"message\": \"Missing or wrong apikey\"}");
            }
        } catch (Exception ex) {
            return ResponseEntity.status(400)
                .body("{\"message\": \"Invalid payload\"}");
        }
    }

    @PostMapping(path="/userall", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postUserAll(@RequestBody String payload) {
        JsonReader reader = Json.createReader(new StringReader(payload));
        JsonObject obj = reader.readObject();
        String id = obj.getString("id");
        List<String> idList = userSvc.getAllUserLogs(id);
        Map<String, List<Transaction>> transById = new HashMap<>();
        JsonObjectBuilder respBuilder = Json.createObjectBuilder();

        // Filter by id
        for(String logId : idList) {
            transById.put(logId, Transaction.stringToTransactions(userSvc.getTransactions(TRANSACTION_ID(logId))));
        }
        JsonObjectBuilder chartBuilder = Json.createObjectBuilder();
        for(String logId : transById.keySet()) {
            JsonObject trends = userSvc.getDailyTrend(transById.get(logId));
            if(!logId.contains("_"))
                trends = userSvc.getMonthlyTrend(transById.get(logId));
            // Get default charts
            chartBuilder.add(logId, Json.createObjectBuilder() 
                .add("trends", trends)
                .add("spending categories", userSvc.getAllCategories(transById.get(logId)))
                .build());   
        }
        try {
            if(obj.getString("apikey").equals(apikey)) {
                return ResponseEntity.status(201)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body((respBuilder.add(id, chartBuilder.build()).build()).toString());
            } else {
                return ResponseEntity.status(400)
                    .body("{\"message\": \"Missing or wrong apikey\"}");
            }
        } catch (Exception ex) {
            return ResponseEntity.status(400)
                .body("{\"message\": \"Invalid payload\"}");
        }
    }

    @GetMapping(path="/status", produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> checkStatus() {
        return userSvc.checkHealth();
    }
}
