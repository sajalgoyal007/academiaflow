package com.studentpro.academiaflow.controller;

import com.studentpro.academiaflow.model.Project;
import com.studentpro.academiaflow.model.ProjectMember;
import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.service.ProjectService;
import com.studentpro.academiaflow.service.TaskService;
import com.studentpro.academiaflow.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List; 

@Controller
@RequestMapping("/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private TaskService taskService;

    private User getAuthenticatedUser(HttpSession session) {
        // Support both session attributes for compatibility
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

    @GetMapping
    public String listProjects(HttpSession session, Model model) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        List<ProjectMember> memberships = projectService.getUserProjects(user);
        model.addAttribute("memberships", memberships);
        return "projects";
    }

    @PostMapping("/create")
    public String createProject(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String dueDate,
                                HttpSession session, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        if (dueDate != null && !dueDate.trim().isEmpty()) {
            project.setDueDate(LocalDate.parse(dueDate));
        }
        projectService.createProject(project, user);
        
        attributes.addFlashAttribute("success", "Project created successfully!");
        return "redirect:/projects";
    }

    @GetMapping("/{id}")
    public String getProjectDetails(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = projectService.getProjectById(id);
        if (project == null) {
            attributes.addFlashAttribute("error", "Project not found!");
            return "redirect:/projects";
        }

        ProjectMember role = projectService.getMemberRole(project, user);
        if (role == null) {
            attributes.addFlashAttribute("error", "Access denied!");
            return "redirect:/projects";
        }

        model.addAttribute("project", project);
        model.addAttribute("role", role.getRole());
        model.addAttribute("members", projectService.getProjectMembers(project));
        model.addAttribute("tasks", taskService.getTasksByProject(project));

        return "project_details";
    }

    @PostMapping("/{id}/edit")
    public String editProject(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String dueDate,
                              HttpSession session, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = projectService.getProjectById(id);
        if (project == null) {
            attributes.addFlashAttribute("error", "Project not found!");
            return "redirect:/projects";
        }

        ProjectMember myRole = projectService.getMemberRole(project, user);
        if (myRole == null || !myRole.getRole().equals("OWNER")) {
            attributes.addFlashAttribute("error", "Only Owner can edit project");
            return "redirect:/projects/" + id;
        }

        project.setName(name);
        project.setDescription(description);
        if (dueDate != null && !dueDate.trim().isEmpty()) {
            project.setDueDate(LocalDate.parse(dueDate));
        } else {
            project.setDueDate(null);
        }
        projectService.updateProject(project);

        attributes.addFlashAttribute("success", "Project updated successfully!");
        return "redirect:/projects/" + id;
    }

    @PostMapping("/{id}/members/add")
    public String addMember(@PathVariable Long id, @RequestParam String email, @RequestParam String roleStr, 
                            HttpSession session, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = projectService.getProjectById(id);
        ProjectMember myRole = projectService.getMemberRole(project, user);

        if (myRole == null || !myRole.getRole().equals("OWNER")) {
            attributes.addFlashAttribute("error", "Only Owner can add members");
            return "redirect:/projects/" + id;
        }

        try {
            projectService.addMemberByEmail(id, email, roleStr);
            attributes.addFlashAttribute("success", "Member added successfully");
        } catch (Exception e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/projects/" + id;
    }
    
    @PostMapping("/{id}/delete")
    public String deleteProject(@PathVariable Long id, HttpSession session, RedirectAttributes attributes) {
        User user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Project project = projectService.getProjectById(id);
        ProjectMember myRole = projectService.getMemberRole(project, user);

        if (myRole == null || !myRole.getRole().equals("OWNER")) {
            attributes.addFlashAttribute("error", "Only Owner can delete project");
            return "redirect:/projects";
        }
        
        taskService.deleteTasksByProject(project);
        projectService.deleteProject(id);
        
        attributes.addFlashAttribute("success", "Project deleted successfully");
        return "redirect:/projects";
    }
}
