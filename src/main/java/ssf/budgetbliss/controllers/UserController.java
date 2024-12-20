package ssf.budgetbliss.controllers;

import static ssf.budgetbliss.models.Constants.*;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import ssf.budgetbliss.models.User;
import ssf.budgetbliss.models.ValidUser;
import ssf.budgetbliss.services.UserService;

@Controller
@RequestMapping
public class UserController {
    
    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserService userSvc;

    // prevents unauthenticated access
    @GetMapping("/home")
    public String getHome(Model model, HttpSession sess) {
        String id = (String) sess.getAttribute(USERID);
        logger.info("[User Controller] In session: " + id);
        if(id == null) {
            logger.info("[User Controller] Directing to /home");
            return "index";
        }
        User user = userSvc.getUserById(id);
        model.addAttribute("user", user);
        return "main";
    }

    @GetMapping("/{userId}/changedetails")
    public ModelAndView changePassword(
        @PathVariable String userId,
        HttpSession sess) {

        ModelAndView mav = new ModelAndView();
        
        if(!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        logger.info("[User Controller] Redirecting to change details");
        mav.setViewName("change-details");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", new ValidUser());
        return mav;
    }

    @PostMapping("/changedetails")
    public ModelAndView postDetails(
        @Valid @ModelAttribute("user") ValidUser user,
        BindingResult bindings,
        HttpSession sess) {

        String id = (String) sess.getAttribute(USERID);

        ModelAndView mav = new ModelAndView();
        if(bindings.hasErrors()) {
            mav.setViewName("change-details");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;

        } else if(!userSvc.changePassword(id, user.getPassword(), user.getConfirmPassword())) {
            logger.info("[User Controller] Password change unsuccessful");
            FieldError err = new FieldError("user", "password", "Incorrect password");
            bindings.addError(err);
            mav.setViewName("change-details");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        }
        userSvc.changePassword(id, user.getPassword(), user.getConfirmPassword());
        if(!id.equals(user.getUserId()))
            userSvc.changeUserId(id, user.getUserId());

        mav.setViewName("successful-change");
        mav.setStatus(HttpStatusCode.valueOf(200));
        return mav;
    }

    @GetMapping("/{userId}/delete")
    public ModelAndView deleteUser(
        @PathVariable String userId,
        HttpSession sess) {

        ModelAndView mav = new ModelAndView();
        
        if(!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        logger.info("[User Controller] Redirecting to delete account");
        mav.setViewName("delete");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", new ValidUser());
        return mav;
    }

    @PostMapping("/delete")
    public ModelAndView postDelete(
        @Valid @ModelAttribute("user") ValidUser user,
        BindingResult bindings,
        HttpSession sess) {

        String id = (String) sess.getAttribute(USERID);
        User currUser = userSvc.getUserById(id);

        ModelAndView mav = new ModelAndView();
        if(bindings.hasErrors()) {
            mav.setViewName("delete");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;

        } else if(!user.getUserId().equals(id)) {
            logger.info("[User Controller] Incorrect user id");
            FieldError err = new FieldError("user", "user", "Incorrect user ID");
            bindings.addError(err);
            mav.setViewName("delete");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;

        } else if (!user.getPassword().equals(currUser.getPassword())) {
            logger.info("[User Controller] Incorrect password");
            FieldError err = new FieldError("user", "password", "Incorrect password");
            bindings.addError(err);
            mav.setViewName("delete");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        }
        userSvc.deleteUser(id);
        sess.invalidate();
        mav.setViewName("index");
        mav.setStatus(HttpStatusCode.valueOf(200));
        return mav;
    }

    @GetMapping("/{userId}/logs") 
    public ModelAndView getUserLogs(
        @PathVariable String userId,
        HttpSession sess) {
        
        ModelAndView mav = new ModelAndView();

        if(!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
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
        model.addAttribute("currList", userSvc.currencyList());
        return "logs";
    }

    @GetMapping("/{userId}/track") 
    public ModelAndView getUserTrack(
        @PathVariable String userId,
        HttpSession sess) {
        
        ModelAndView mav = new ModelAndView();

        if(!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        logger.info("[User Controller] Redirecting to user track");
        User user = userSvc.getUserById(userId);
        mav.setViewName("track");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", user);
        mav.addObject("imgList", userSvc.getDefaultCharts(user));
        mav.addObject("transactions", user.getTransactions().subList(0, 5));

        return mav;
    }

    @GetMapping("/{userId}/info") 
    public ModelAndView getUserInfo(
        @PathVariable String userId,
        HttpSession sess) {
        
        ModelAndView mav = new ModelAndView();
     
        if(!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
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
