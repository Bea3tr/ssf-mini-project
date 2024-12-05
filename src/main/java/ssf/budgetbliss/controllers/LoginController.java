package ssf.budgetbliss.controllers;

import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import ssf.budgetbliss.models.User;
import ssf.budgetbliss.models.ValidUser;
import ssf.budgetbliss.services.UserService;

import static ssf.budgetbliss.models.Constants.*;

@Controller
@RequestMapping
public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @Autowired
    private UserService userSvc;

    @GetMapping("/login")
    public String getLogin(Model model) {
        model.addAttribute("user", new ValidUser());
        return "login";
    }

    @PostMapping("/home")
    public ModelAndView login(
        @Valid @ModelAttribute ValidUser user,
        BindingResult bindings,
        HttpSession sess) {

        ModelAndView mav = new ModelAndView();

        Optional<User> opt = userSvc.getUser(user.getUserId(), user.getPassword());
        if(!userSvc.userExists(user.getUserId())) {
            logger.info("[Controller] User " + user.getUserId() + " does not exist");
            FieldError err = new FieldError("user", "userId", "User does not exist");
            bindings.addError(err);
            mav.setViewName("login");
            mav.setStatus(HttpStatusCode.valueOf(404));
            mav.addObject("user", new ValidUser());
            return mav;
        }
        if(opt.isEmpty()) {
            FieldError err = new FieldError("user", "password", "Incorrect password");
            bindings.addError(err);
            mav.setViewName("login");
            mav.setStatus(HttpStatusCode.valueOf(401));
            mav.addObject("user", new ValidUser());
            return mav;
        }
        // Add user to session only when login is successful
        String userId = (String) sess.getAttribute(USERID);
        if(userId == null) {
            userId = user.getUserId();
            sess.setAttribute(USERID, userId);
        }
        User userSuccess = opt.get();
        logger.info("[Controller] User login successful");
        mav.setViewName("main");
        mav.setStatus(HttpStatusCode.valueOf(200));
        mav.addObject("user", userSuccess);
        return mav;
    }
    
}