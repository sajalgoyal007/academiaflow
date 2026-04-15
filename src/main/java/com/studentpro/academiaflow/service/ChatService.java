package com.studentpro.academiaflow.service;

import com.studentpro.academiaflow.model.ProjectMember;
import com.studentpro.academiaflow.model.Task;
import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.repository.ProjectMemberRepository;
import com.studentpro.academiaflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private TaskRepository taskRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Build a context string with all of the user's project and task data
     */
    public String buildProjectContext(User user) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("=== USER INFO ===\n");
        ctx.append("Name: ").append(user.getName()).append("\n");
        ctx.append("Email: ").append(user.getEmail()).append("\n");
        if (user.getCollege() != null) ctx.append("College: ").append(user.getCollege()).append("\n");
        if (user.getBranch() != null) ctx.append("Branch: ").append(user.getBranch()).append("\n");
        if (user.getYear() != null) ctx.append("Year: ").append(user.getYear()).append("\n");
        if (user.getSkills() != null) ctx.append("Skills: ").append(user.getSkills()).append("\n");
        if (user.getInterests() != null) ctx.append("Interests: ").append(user.getInterests()).append("\n");
        if (user.getBio() != null) ctx.append("Bio: ").append(user.getBio()).append("\n");

        // Fetch projects
        List<ProjectMember> memberships = memberRepository.findByUser(user);
        ctx.append("\n=== PROJECTS (").append(memberships.size()).append(" total) ===\n");

        for (ProjectMember pm : memberships) {
            ctx.append("\n--- Project: ").append(pm.getProject().getName()).append(" ---\n");
            ctx.append("  Role: ").append(pm.getRole()).append("\n");
            if (pm.getProject().getDescription() != null) {
                ctx.append("  Description: ").append(pm.getProject().getDescription()).append("\n");
            }
            if (pm.getProject().getDueDate() != null) {
                ctx.append("  Due Date: ").append(pm.getProject().getDueDate()).append("\n");
            }

            // Fetch tasks for this project
            List<Task> tasks = taskRepository.findByProjectOrderByCreatedAtDesc(pm.getProject());
            if (!tasks.isEmpty()) {
                ctx.append("  Tasks (").append(tasks.size()).append("):\n");
                for (Task t : tasks) {
                    ctx.append("    - [").append(t.getStatus()).append("] ").append(t.getTitle());
                    ctx.append(" (Priority: ").append(t.getPriority()).append(")");
                    if (t.getAssignedTo() != null) {
                        ctx.append(" → Assigned to: ").append(t.getAssignedTo().getName());
                    }
                    if (t.getDueDate() != null) {
                        ctx.append(" Due: ").append(t.getDueDate());
                    }
                    ctx.append("\n");
                    if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                        ctx.append("      Desc: ").append(t.getDescription()).append("\n");
                    }
                }
            } else {
                ctx.append("  Tasks: None yet\n");
            }
        }

        // Summary stats
        long totalTasks = taskRepository.countAllTasksForUser(user.getId());
        long completed = taskRepository.countCompletedTasksForUser(user.getId());
        long pending = taskRepository.countPendingTasksForUser(user.getId());
        ctx.append("\n=== STATS ===\n");
        ctx.append("Total Projects: ").append(memberships.size()).append("\n");
        ctx.append("Total Tasks: ").append(totalTasks).append("\n");
        ctx.append("Completed Tasks: ").append(completed).append("\n");
        ctx.append("Pending Tasks: ").append(pending).append("\n");

        return ctx.toString();
    }

    /**
     * Send a message to Groq AI with project context
     */
    public String chat(User user, String userMessage) {
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            return "⚠️ Groq API key is not configured. Please add `groq.api.key=your_key` to application.properties.";
        }

        try {
            String projectContext = buildProjectContext(user);

            String systemPrompt = "You are **AcademiaFlow AI Assistant**, a helpful project management assistant for a student named "
                    + user.getName() + ". "
                    + "You help them manage their academic projects, tasks, and deadlines. "
                    + "You have full access to their project data shown below. "
                    + "Be concise, friendly, and proactive with suggestions. "
                    + "If they ask about their projects/tasks, use the real data below. "
                    + "If they need help with coding, studying, or planning, help them. "
                    + "Always respond in a helpful, structured way using short paragraphs or bullet points.\n\n"
                    + "--- STUDENT'S DATA ---\n"
                    + projectContext;

            // Build JSON payload (manually to avoid extra dependencies)
            String jsonPayload = "{"
                    + "\"model\": \"" + escapeJson(groqModel) + "\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"" + escapeJson(systemPrompt) + "\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(userMessage) + "\"}"
                    + "],"
                    + "\"temperature\": 0.7,"
                    + "\"max_tokens\": 1024"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractContent(response.body());
            } else {
                return "⚠️ Groq API error (HTTP " + response.statusCode() + "): " + extractErrorMessage(response.body());
            }

        } catch (Exception e) {
            return "⚠️ Error connecting to AI: " + e.getMessage();
        }
    }

    /**
     * Extract the assistant's message content from the JSON response
     */
    private String extractContent(String jsonResponse) {
        // Simple JSON parsing without external library
        int contentIdx = jsonResponse.indexOf("\"content\":");
        if (contentIdx == -1) return "No response from AI.";

        // Find the start of the content string
        int startQuote = jsonResponse.indexOf("\"", contentIdx + 10);
        if (startQuote == -1) return "No response from AI.";

        // Find the end of the content string (handle escaped quotes)
        StringBuilder content = new StringBuilder();
        int i = startQuote + 1;
        while (i < jsonResponse.length()) {
            char c = jsonResponse.charAt(i);
            if (c == '\\' && i + 1 < jsonResponse.length()) {
                char next = jsonResponse.charAt(i + 1);
                if (next == '"') {
                    content.append('"');
                    i += 2;
                } else if (next == 'n') {
                    content.append('\n');
                    i += 2;
                } else if (next == 't') {
                    content.append('\t');
                    i += 2;
                } else if (next == '\\') {
                    content.append('\\');
                    i += 2;
                } else {
                    content.append(c);
                    i++;
                }
            } else if (c == '"') {
                break;
            } else {
                content.append(c);
                i++;
            }
        }

        return content.toString();
    }

    private String extractErrorMessage(String jsonResponse) {
        try {
            int msgIdx = jsonResponse.indexOf("\"message\":");
            if (msgIdx == -1) return jsonResponse.substring(0, Math.min(200, jsonResponse.length()));
            int startQ = jsonResponse.indexOf("\"", msgIdx + 10);
            int endQ = jsonResponse.indexOf("\"", startQ + 1);
            return jsonResponse.substring(startQ + 1, endQ);
        } catch (Exception e) {
            return jsonResponse.substring(0, Math.min(200, jsonResponse.length()));
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
