package ssf.budgetbliss.controllers;

import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import ssf.budgetbliss.models.User;
import ssf.budgetbliss.models.ValidUser;
import ssf.budgetbliss.services.UserService;

import static ssf.budgetbliss.models.Constants.*;

@Controller
@RequestMapping("/signup")
public class SignUpController {

    private static final Logger logger = Logger.getLogger(SignUpController.class.getName());

    @Autowired 
    private UserService userSvc;

    @GetMapping
    public String getSignup(Model model, HttpSession sess) {
        logger.info("[SignUp Controller] Redirecting to signup page");
        if (sess.getAttribute(USERID) != null) {
            sess.invalidate();
        }
        model.addAttribute("user", new ValidUser());
        return "signup";
    }

    @PostMapping("/home") 
    public String signup(Model model,
        @Valid @ModelAttribute("user") ValidUser user,
        BindingResult bindings,
        HttpSession sess) {

        if(bindings.hasErrors())
            return "signup";

        if(userSvc.userExists(user.getUserId())) {
            logger.info("[SignUp Controller] User exists");
            FieldError err = new FieldError("user", "userId", "User exists, enter another id");
            bindings.addError(err);
            return "signup";
        }

        if(!user.getPassword().equals(user.getConfirmPassword())) {
            logger.info("[SignUp Controller] Sign up unsuccessful");
            FieldError err = new FieldError("user", "confirmPassword", "Password does not match");
            bindings.addError(err);
            return "signup";
        }
        // Add user to session only when signup is successful
        String userId = (String) sess.getAttribute(USERID);
        if(userId == null) {
            userId = user.getUserId();
            sess.setAttribute(USERID, userId);
        }
        userSvc.insertUser(userId, user.getPassword());
        logger.info("[SignUp Controller] Sign up successful");
        Optional<User> opt = userSvc.getUser(user.getUserId(), user.getPassword());
        User userSuccess = opt.get();
        model.addAttribute("user", userSuccess);
        return "main";
    }
    
}
