package com.studentpro.academiaflow.controller;

import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.repository.ProjectMemberRepository;
import com.studentpro.academiaflow.repository.TaskRepository;
import com.studentpro.academiaflow.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private TaskRepository taskRepository;

    private User getSessionUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getId() != null) {
            return userService.findById(user.getId());
        }
        return null;
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        session.setAttribute("user", user);
        model.addAttribute("user", user);

        try {
            model.addAttribute("totalProjects", memberRepository.countByUser(user));
            model.addAttribute("totalTasks", taskRepository.countAllTasksForUser(user.getId()));
            model.addAttribute("completedTasks", taskRepository.countCompletedTasksForUser(user.getId()));
            model.addAttribute("pendingTasks", taskRepository.countPendingTasksForUser(user.getId()));
        } catch (Exception e) {
            model.addAttribute("totalProjects", 0);
            model.addAttribute("totalTasks", 0);
            model.addAttribute("completedTasks", 0);
            model.addAttribute("pendingTasks", 0);
        }

        return "profile";
    }

    @GetMapping("/profile/edit")
    public String showEditProfile(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        return "edit_profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@RequestParam String name,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String college,
                                @RequestParam(required = false) String branch,
                                @RequestParam(required = false) String year,
                                @RequestParam(required = false) String skills,
                                @RequestParam(required = false) String interests,
                                @RequestParam(required = false) String bio,
                                HttpSession session, RedirectAttributes attributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        user.setName(name);
        user.setPhone(phone);
        user.setCollege(college);
        user.setBranch(branch);
        user.setYear(year);
        user.setSkills(skills);
        user.setInterests(interests);
        user.setBio(bio);

        User updated = userService.updateProfile(user);
        session.setAttribute("user", updated);

        attributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/profile";
    }

    @GetMapping("/profile/change-password")
    public String showChangePassword(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        return "change_password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session, RedirectAttributes attributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        if (!newPassword.equals(confirmPassword)) {
            attributes.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/profile/change-password";
        }

        if (newPassword.length() < 6) {
            attributes.addFlashAttribute("error", "Password must be at least 6 characters");
            return "redirect:/profile/change-password";
        }

        boolean changed = userService.changePassword(user, oldPassword, newPassword);
        if (changed) {
            session.setAttribute("user", userService.findById(user.getId()));
            attributes.addFlashAttribute("success", "Password changed successfully!");
            return "redirect:/profile";
        } else {
            attributes.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/profile/change-password";
        }
    }
}
