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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getId() == null) {
            return "redirect:/login";
        }

        // Refresh user from DB
        User freshUser = userService.findById(user.getId());
        if (freshUser == null) {
            session.invalidate();
            return "redirect:/login";
        }

        session.setAttribute("user", freshUser);
        model.addAttribute("user", freshUser);

        try {
            long totalProjects = memberRepository.countByUser(freshUser);
            long totalTasks = taskRepository.countAllTasksForUser(freshUser.getId());
            long completedTasks = taskRepository.countCompletedTasksForUser(freshUser.getId());
            long pendingTasks = taskRepository.countPendingTasksForUser(freshUser.getId());

            model.addAttribute("totalProjects", totalProjects);
            model.addAttribute("totalTasks", totalTasks);
            model.addAttribute("completedTasks", completedTasks);
            model.addAttribute("pendingTasks", pendingTasks);

            // Daily Productivity Score
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            long completedToday = taskRepository.countCompletedTodayForUser(freshUser.getId(), startOfDay);
            long createdToday = taskRepository.countCreatedTodayForUser(freshUser.getId(), startOfDay);
            
            // Score: ratio of completed to total, weighted
            int productivityScore;
            if (totalTasks == 0) {
                productivityScore = 0;
            } else if (pendingTasks == 0 && completedTasks > 0) {
                productivityScore = 100;
            } else {
                // Base: overall completion rate * 70 + today's activity bonus * 30
                int overallRate = (int) (completedTasks * 100 / totalTasks);
                int todayBonus = (completedToday > 0) ? Math.min(100, (int)(completedToday * 25)) : 0;
                productivityScore = Math.min(100, (overallRate * 70 + todayBonus * 30) / 100);
            }

            model.addAttribute("productivityScore", productivityScore);
            model.addAttribute("completedToday", completedToday);
            model.addAttribute("createdToday", createdToday);

        } catch (Exception e) {
            model.addAttribute("totalProjects", 0);
            model.addAttribute("totalTasks", 0);
            model.addAttribute("completedTasks", 0);
            model.addAttribute("pendingTasks", 0);
            model.addAttribute("productivityScore", 0);
            model.addAttribute("completedToday", 0);
            model.addAttribute("createdToday", 0);
        }

        return "dashboard";
    }
}
