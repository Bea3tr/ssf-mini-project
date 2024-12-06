package ssf.budgetbliss.controllers;

import static ssf.budgetbliss.models.Constants.*;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;
import ssf.budgetbliss.models.User;
import ssf.budgetbliss.services.UserService;

@Controller
@RequestMapping
public class UserController {
    
    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserService userSvc;

    // prevents unauthenticated access
    @GetMapping("/home")
    public String getHome() {
        logger.info("[User Controller] Directing to /home");
        return "index";
    }

    @GetMapping("/{userId}/logs") 
    public ModelAndView getUserLogs(
        @PathVariable String userId,
        HttpSession sess) {
        
        ModelAndView mav = new ModelAndView();
        String id = (String) sess.getAttribute(USERID);
        if(id == null) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        } else if (!userId.equals(id)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        } 
        logger.info("[User Controller] Redirecting to user logs");
        User user = userSvc.getUserById(userId);
        mav.setViewName("logs");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", user);
        mav.addObject("currList", userSvc.currencyList());

        return mav;
    }

    @PostMapping("/logs")
    public String postLogs(Model model,
        @RequestBody MultiValueMap<String, String> form,
        HttpSession sess) {
        
        String userId = (String)sess.getAttribute(USERID);

        String cashflow = form.getFirst("cashflow");
        String transType = form.getFirst("transtype");
        String currency = form.getFirst("currency");
        float amt = Float.parseFloat(form.getFirst("amt"));
        userSvc.updateBal(userId, currency, cashflow, transType, amt);

        User user = userSvc.getUserById(userId);
        model.addAttribute("user", user);
        return "logs";
    }

    @GetMapping("/{userid}/track") 
    public ModelAndView getUserTrack(
        @PathVariable String userId,
        HttpSession sess) {
        
        ModelAndView mav = new ModelAndView();
        String id = (String) sess.getAttribute(USERID);
        if(id == null) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        } else if (!userId.equals(id)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        } 
        logger.info("[User Controller] Redirecting to user track");
        User user = userSvc.getUserById(userId);
        mav.setViewName("track");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", user);

        return mav;
    }

    @GetMapping("/{userid}/info") 
    public ModelAndView getUserInfo(
        @PathVariable String userId,
        HttpSession sess) {
        
        ModelAndView mav = new ModelAndView();
        String id = (String) sess.getAttribute(USERID);
        if(id == null) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        } else if (!userId.equals(id)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        } 
        logger.info("[User Controller] Redirecting to user info");
        User user = userSvc.getUserById(userId);
        mav.setViewName("info");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", user);

        return mav;
    }

    
}
