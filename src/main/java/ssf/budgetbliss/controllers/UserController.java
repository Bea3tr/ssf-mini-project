package ssf.budgetbliss.controllers;

import static ssf.budgetbliss.models.Constants.*;

import java.util.logging.Logger;
import java.text.ParseException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        if (id == null) {
            logger.info("[User Controller] Directing to /home");
            return "index";
        }
        User user = userSvc.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("logs", userSvc.getTravelLogs(id));
        return "main";
    }

    @GetMapping("/{userId}/changedetails")
    public ModelAndView changePassword(
            @PathVariable String userId,
            HttpSession sess) {

        ModelAndView mav = new ModelAndView();

        if (!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        logger.info("[User Controller] Redirecting to change details");
        mav.setViewName("change-details");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", new ValidUser());
        mav.addObject("currList", userSvc.currencyList());
        return mav;
    }

    @PostMapping("/changedetails")
    public ModelAndView postDetails(
            @Valid @ModelAttribute("user") ValidUser user,
            BindingResult bindings,
            @RequestBody MultiValueMap<String, String> form,
            HttpSession sess) {

        String id = (String) sess.getAttribute(USERID);

        ModelAndView mav = new ModelAndView();
        if (bindings.hasErrors()) {
            mav.setViewName("change-details");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;

        } else if (!userSvc.changePassword(id, user.getPassword(), user.getConfirmPassword())) {
            logger.info("[User Controller] Password change unsuccessful");
            FieldError err = new FieldError("user", "password", "Incorrect password");
            bindings.addError(err);
            mav.setViewName("change-details");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;
        }
        userSvc.changePassword(id, user.getPassword(), user.getConfirmPassword());
        userSvc.updateCurr(id, form.getFirst("defCurr"));
        if (!id.equals(user.getUserId()))
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

        if (!userSvc.isAuth(sess, userId)) {
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
        if (bindings.hasErrors()) {
            mav.setViewName("delete");
            mav.setStatus(HttpStatusCode.valueOf(400));
            return mav;

        } else if (!user.getUserId().equals(id)) {
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

    @GetMapping(path={"/{userId}/logs", "/{userId}/{travelId}/logs"})
    public ModelAndView getUserLogs(
            @PathVariable String userId,
            @PathVariable(required=false) String travelId,
            HttpSession sess) {

        ModelAndView mav = new ModelAndView();

        if (!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        if(travelId == null) {
            logger.info("[User Controller] Redirecting to user logs");
            User user = userSvc.getUserById(userId);
            mav.setViewName("logs");
            mav.addObject("user", user);
            mav.addObject("transactions", user.getTransactions());
        } else {
            logger.info("[User Controller] Redirecting to travel logs");
            User user = userSvc.getUserById(travelId);
            mav.setViewName("travel");
            mav.addObject("user", user);
            mav.addObject("userId", userId);
            mav.addObject("transactions", user.getTransactions());
        }
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("currList", userSvc.currencyList());
        mav.addObject("months", MONTHS);
        mav.addObject("years", YEARS);
        return mav;
    }

    @PostMapping("/logs")
    public String postLogs(Model model,
            @RequestBody MultiValueMap<String, String> form,
            HttpSession sess) {

        String userId = (String) sess.getAttribute(USERID);

        String cashflow = form.getFirst("cashflow");
        String transType = form.getFirst("transtype");
        String currency = form.getFirst("currency");
        float amt = Float.parseFloat(form.getFirst("amt"));
        Date date = new Date();
        try {
            date = DF.parse(form.getFirst("date"));
        } catch (ParseException ex) {
            logger.info("[User Controller] Error parsing date input");
            ex.printStackTrace();
        }

        userSvc.updateBal(userId, currency, cashflow, transType, amt, date, false);

        User user = userSvc.getUserById(userId);
        model.addAttribute("user", user);
        model.addAttribute("currList", userSvc.currencyList());
        model.addAttribute("transactions", user.getTransactions());
        model.addAttribute("months", MONTHS);
        model.addAttribute("years", YEARS);
        return "logs";
    }

    // Creating new travel log
    @PostMapping("/{userId}/travel")
    public ModelAndView postUserTravel(
            @PathVariable String userId,
            @RequestBody MultiValueMap<String, String> form,
            BindingResult bindings,
            HttpSession sess) {

        ModelAndView mav = new ModelAndView();
        mav.addObject("months", MONTHS);
        mav.addObject("years", YEARS);

        // String name = form.getFirst("name");
        if(userSvc.userExists(TRAVEL_ID(userId, form.getFirst("name"))) || form.getFirst("name").contains("transactions")) {
            logger.info("[User Controller - Post travel] Name exists");
            User user = userSvc.getUserById(userId);
            ObjectError err = new ObjectError("name", "Invalid name. Name in use or name contains 'transactions'");
            bindings.addError(err);
            mav.setViewName("logs");
            mav.setStatus(HttpStatusCode.valueOf(400));
            mav.addObject("user", user);
            mav.addObject("transactions", user.getTransactions());
            mav.addObject("currList", userSvc.currencyList());
            mav.addObject("error", bindings.getAllErrors());
            return mav;
        }
        logger.info("[User Controller] Inserting currency details");
        userSvc.insertUserTrip(userId, form.getFirst("name"), form.getFirst("destCurr"));
        logger.info("[User Controller] Redirecting to travel logs");
        User user = userSvc.getUserById(TRAVEL_ID(userId, form.getFirst("name")));
        mav.setViewName("travel");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", user);
        mav.addObject("userId", userId);
        mav.addObject("transactions", user.getTransactions());
        
        return mav;
    }

    // Adding new records to travel log
    @PostMapping("/travel")
    public String postTravel(Model model,
            @RequestBody MultiValueMap<String, String> form,
            HttpSession sess) {

        String userId = (String) sess.getAttribute(USERID);
        
        String travelId = form.getFirst("travelId");
        String cashflow = form.getFirst("cashflow");
        String currency = form.getFirst("currency");
        String transType = form.getFirst("transtype");
        float amt = Float.parseFloat(form.getFirst("amt"));
        logger.info("[User Controler] Hidden inputs: " + travelId + " " + currency);
        Date date = new Date();
        try {
            date = DF.parse(form.getFirst("date"));
        } catch (ParseException ex) {
            logger.info("[User Controller] Error parsing date input");
            ex.printStackTrace();
        }

        userSvc.updateBal(travelId, currency, cashflow, transType, amt, date, false);

        User user = userSvc.getUserById(travelId);
        model.addAttribute("user", user);
        model.addAttribute("userId", userId);
        model.addAttribute("months", MONTHS);
        model.addAttribute("years", YEARS);
        model.addAttribute("transactions", user.getTransactions());
        return "travel";
    }

    @GetMapping(path={"/{userId}/edit", "/{userId}/{travelId}/edit"})
    public ModelAndView getEdit(@PathVariable String userId,
            @PathVariable(required=false) String travelId,
            @RequestParam("index") int index,
            HttpSession sess) {

        ModelAndView mav = new ModelAndView();

        if (!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        mav.setViewName("edit");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("userId", userId);
        mav.addObject("index", index);
        mav.addObject("currList", userSvc.currencyList());

        if(travelId != null)
            mav.addObject("transId", travelId);
        else 
            mav.addObject("transId", userId);
    
        return mav;
    }

    @PostMapping("/edit")
    public String postEdit(Model model,
            @RequestBody MultiValueMap<String, String> form,
            HttpSession sess) {

        String userId = (String) sess.getAttribute(USERID);
        
        String transId = form.getFirst("transId");
        String cashflow = form.getFirst("cashflow");
        String currency = form.getFirst("currency");
        String transType = form.getFirst("transtype");
        int index = Integer.parseInt(form.getFirst("index"));
        float amt = Float.parseFloat(form.getFirst("amt"));
        Date date = new Date();
        try {
            date = DF.parse(form.getFirst("date"));
        } catch (ParseException ex) {
            logger.info("[User Controller] Error parsing date input");
            ex.printStackTrace();
        }
        userSvc.editTransaction(transId, index, userSvc.createTransaction(cashflow, currency, transType, amt, date));
        User user = userSvc.getUserById(transId);
        model.addAttribute("user", user);
        model.addAttribute("months", MONTHS);
        model.addAttribute("years", YEARS);
        model.addAttribute("currList", userSvc.currencyList());
        model.addAttribute("transactions", user.getTransactions());
        if(transId.contains("_")) {
            model.addAttribute("userId", userId);
            return "travel";
        }
        return "logs";
    }

    @GetMapping(path={"/{userId}/filtered", "/{userId}/{travelId}/filtered"})
    public ModelAndView getFiltered(@PathVariable String userId,
            @PathVariable(required=false) String travelId,
            @RequestParam MultiValueMap<String, String> form,
            HttpSession sess) {

        ModelAndView mav = new ModelAndView();

        if (!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        if(form.getFirst("travelId") != null) 
            userId = form.getFirst("travelId");
        
        int year = Integer.parseInt(form.getFirst("year"));
        int month = Integer.parseInt(form.getFirst("month"));

        User user = userSvc.getUserById(userId);
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", user);
        mav.addObject("currList", userSvc.currencyList());
        mav.addObject("months", MONTHS);
        mav.addObject("years", YEARS);
        mav.addObject("transactions", userSvc.getFilteredTransactions(TRANSACTION_ID(userId), year, month));
        if(userId.contains("_")){
            mav.addObject("userId", userId);
            mav.setViewName("travel");
        } else {
            mav.setViewName("logs");
        }
        return mav;
    }

    @GetMapping("/{userId}/track")
    public ModelAndView getUserTrack(
            @PathVariable String userId,
            HttpSession sess) {

        ModelAndView mav = new ModelAndView();
        int toIndex = 5;

        if (!userSvc.isAuth(sess, userId)) {
            logger.info("[User Controller] Unauthenticated access");
            mav.setViewName("not-login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            return mav;
        }
        logger.info("[User Controller] Redirecting to user track");
        User user = userSvc.getUserById(userId);
        if(user.getTransactions().size() < toIndex)
            toIndex = user.getTransactions().size();
        mav.setViewName("track");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", user);
        mav.addObject("imgList", userSvc.getDefaultCharts(user));
        mav.addObject("transactions", user.getTransactions().subList(0, toIndex));

        return mav;
    }

    @GetMapping("/{userId}/info")
    public ModelAndView getUserInfo(
            @PathVariable String userId,
            HttpSession sess) {

        ModelAndView mav = new ModelAndView();

        if (!userSvc.isAuth(sess, userId)) {
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

    @PostMapping("/delete-transaction")
    public String postDeleteTransaction(Model model,
        @RequestBody MultiValueMap<String, String> form,
        HttpSession sess) {

        String id = form.getFirst("id");
        logger.info("[User Controller] Transaction to delete: " + form.getFirst("transaction"));
        userSvc.deleteTransaction(id, form.getFirst("transaction"));

        User user = userSvc.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("currList", userSvc.currencyList());
        model.addAttribute("months", MONTHS);
        model.addAttribute("years", YEARS);
        model.addAttribute("transactions", user.getTransactions());
        if(id.contains("_")){
            model.addAttribute("userId", sess.getAttribute(USERID));
            return "travel";
        }
        return "logs";
    }

}
