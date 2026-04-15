package com.studentpro.academiaflow.controller;

import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, 
                        @RequestParam String password, 
                        HttpSession session, 
                        RedirectAttributes redirectAttributes) {
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email is required");
            return "redirect:/login";
        }
        if (password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Password is required");
            return "redirect:/login";
        }
        
        // Check if user exists first
        User existingUser = userService.findByEmail(email.trim());
        if (existingUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found with this email");
            return "redirect:/login";
        }

        User user = userService.authenticate(email.trim(), password);
        if (user != null) {
            // Store both user object AND userId for consistent access
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            return "redirect:/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("error", "Incorrect password");
            return "redirect:/login";
        }
    }

    @GetMapping("/signup")
    public String showSignupForm(HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String name,
                         @RequestParam String email, 
                         @RequestParam String password, 
                         RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(name, email, password);
            redirectAttributes.addFlashAttribute("success", "Registration successful. Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/signup";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
