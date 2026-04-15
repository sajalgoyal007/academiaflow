package com.studentpro.academiaflow.controller;

import com.studentpro.academiaflow.model.ProjectMember;
import com.studentpro.academiaflow.model.Task;
import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.repository.ProjectMemberRepository;
import com.studentpro.academiaflow.repository.TaskRepository;
import com.studentpro.academiaflow.service.TaskService;
import com.studentpro.academiaflow.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ReportController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @GetMapping("/report/download")
    public void downloadReport(HttpSession session, HttpServletResponse response) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || user.getId() == null) {
                response.sendRedirect("/login");
                return;
            }

            User freshUser = userService.findById(user.getId());
            if (freshUser == null) {
                response.sendRedirect("/login");
                return;
            }

            // Set response headers for file download
            String filename = "AcademiaFlow_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".txt";
            response.setContentType("text/plain; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            PrintWriter writer = response.getWriter();

            // Header
            writer.println("╔══════════════════════════════════════════════════════════╗");
            writer.println("║             ACADEMIAFLOW - PROJECT REPORT               ║");
            writer.println("╚══════════════════════════════════════════════════════════╝");
            writer.println();
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));
            writer.println();

            // User Info
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  STUDENT PROFILE");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  Name     : " + freshUser.getName());
            writer.println("  Email    : " + freshUser.getEmail());
            if (freshUser.getCollege() != null) writer.println("  College  : " + freshUser.getCollege());
            if (freshUser.getBranch() != null) writer.println("  Branch   : " + freshUser.getBranch());
            if (freshUser.getYear() != null) writer.println("  Year     : " + freshUser.getYear());
            if (freshUser.getSkills() != null) writer.println("  Skills   : " + freshUser.getSkills());
            writer.println();

            // Summary stats
            long totalProjects = memberRepository.countByUser(freshUser);
            long totalTasks = taskRepository.countAllTasksForUser(freshUser.getId());
            long completedTasks = taskRepository.countCompletedTasksForUser(freshUser.getId());
            long pendingTasks = taskRepository.countPendingTasksForUser(freshUser.getId());
            int completionRate = totalTasks > 0 ? (int) (completedTasks * 100 / totalTasks) : 0;

            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  SUMMARY");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  Total Projects   : " + totalProjects);
            writer.println("  Total Tasks      : " + totalTasks);
            writer.println("  Completed Tasks  : " + completedTasks);
            writer.println("  Pending Tasks    : " + pendingTasks);
            writer.println("  Completion Rate  : " + completionRate + "%");
            writer.println();

            // Projects & Tasks
            List<ProjectMember> memberships = memberRepository.findByUser(freshUser);
            for (ProjectMember pm : memberships) {
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                writer.println("  PROJECT: " + pm.getProject().getName());
                writer.println("  Role: " + pm.getRole());
                if (pm.getProject().getDescription() != null) {
                    writer.println("  Description: " + pm.getProject().getDescription());
                }
                if (pm.getProject().getDueDate() != null) {
                    writer.println("  Due Date: " + pm.getProject().getDueDate());
                }
                writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                List<Task> tasks = taskRepository.findByProjectOrderByCreatedAtDesc(pm.getProject());
                if (tasks.isEmpty()) {
                    writer.println("  (No tasks)");
                } else {
                    writer.println();
                    writer.printf("  %-4s %-30s %-12s %-10s %-12s%n", "#", "TASK", "STATUS", "PRIORITY", "DUE DATE");
                    writer.println("  " + "-".repeat(70));

                    int count = 1;
                    for (Task t : tasks) {
                        String title = t.getTitle().length() > 28 ? t.getTitle().substring(0, 28) + ".." : t.getTitle();
                        String status = t.getStatus();
                        String priority = t.getPriority();
                        String due = t.getDueDate() != null ? t.getDueDate().toString() : "-";
                        writer.printf("  %-4d %-30s %-12s %-10s %-12s%n", count++, title, status, priority, due);
                    }
                }
                writer.println();
            }

            // Footer
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  Generated by AcademiaFlow | Built by Sajal Goyal");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            writer.flush();

        } catch (Exception e) {
            try {
                response.sendRedirect("/dashboard");
            } catch (Exception ignored) {}
        }
    }
}
