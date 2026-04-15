package com.studentpro.academiaflow.controller;

import com.studentpro.academiaflow.model.Project;
import com.studentpro.academiaflow.model.ProjectMember;
import com.studentpro.academiaflow.model.Task;
import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.service.ProjectService;
import com.studentpro.academiaflow.service.TaskService;
import com.studentpro.academiaflow.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/projects/{projectId}/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    private User getAuthenticatedUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getId() != null) {
            return user;
        }
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            return userService.findById(userId);
        }
        return null;
    }

    private boolean canManageTask(ProjectMember memberRole) {
        return memberRole != null && (memberRole.getRole().equals("OWNER") || memberRole.getRole().equals("MEMBER"));
    }

    @PostMapping("/create")
    public String createTask(@PathVariable Long projectId,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam String priority,
                             @RequestParam(required = false) String dueDate,
                             @RequestParam(required = false) Long assignedTo,
                             HttpSession session, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            attributes.addFlashAttribute("error", "Project not found!");
            return "redirect:/projects";
        }

        ProjectMember role = projectService.getMemberRole(project, user);
        if (!canManageTask(role)) {
            attributes.addFlashAttribute("error", "You do not have permission to create tasks.");
            return "redirect:/projects/" + projectId;
        }

        Task task = new Task();
        task.setProject(project);
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);

        if (dueDate != null && !dueDate.trim().isEmpty()) {
            task.setDueDate(LocalDate.parse(dueDate));
        }

        if (assignedTo != null && assignedTo > 0) {
            task.setAssignedTo(userService.findById(assignedTo));
        }

        taskService.saveTask(task);
        attributes.addFlashAttribute("success", "Task created successfully!");
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{taskId}/status")
    public String updateStatus(@PathVariable Long projectId, @PathVariable Long taskId,
                               @RequestParam String status,
                               HttpSession session, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = projectService.getProjectById(projectId);
        ProjectMember role = projectService.getMemberRole(project, user);

        if (!canManageTask(role)) {
            attributes.addFlashAttribute("error", "You do not have permission to update tasks.");
            return "redirect:/projects/" + projectId;
        }

        Task task = taskService.getTaskById(taskId);
        if (task != null && task.getProject().getId().equals(projectId)) {
            task.setStatus(status);
            taskService.saveTask(task);
            attributes.addFlashAttribute("success", "Task status updated!");
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{taskId}/delete")
    public String deleteTask(@PathVariable Long projectId, @PathVariable Long taskId,
                             HttpSession session, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = projectService.getProjectById(projectId);
        ProjectMember role = projectService.getMemberRole(project, user);

        if (!canManageTask(role)) {
            attributes.addFlashAttribute("error", "You do not have permission to delete tasks.");
            return "redirect:/projects/" + projectId;
        }

        Task task = taskService.getTaskById(taskId);
        if (task != null && task.getProject().getId().equals(projectId)) {
            taskService.deleteTask(taskId);
            attributes.addFlashAttribute("success", "Task deleted!");
        }

        return "redirect:/projects/" + projectId;
    }
}
