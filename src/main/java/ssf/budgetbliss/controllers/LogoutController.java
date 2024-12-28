package ssf.budgetbliss.controllers;

import static ssf.budgetbliss.models.Constants.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/logout")
public class LogoutController {

    @GetMapping
    public String getLogout(Model model, HttpSession sess) {

        String userId = (String) sess.getAttribute(USERID);
        if(userId == null) {
            return "logout-fail";
        }
        sess.invalidate();
        return "logout";
    }

    @PostMapping
    public String postLogout(Model model, HttpSession sess) {
        sess.invalidate();
        return "logout";
    }
    
}
