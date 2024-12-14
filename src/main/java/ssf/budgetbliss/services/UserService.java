package ssf.budgetbliss.services;

import static ssf.budgetbliss.models.Constants.CURR_URL;

import java.io.StringReader;
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

}
