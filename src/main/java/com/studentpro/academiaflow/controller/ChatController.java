package com.studentpro.academiaflow.controller;

import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.service.ChatService;
import com.studentpro.academiaflow.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request,
                                                     HttpSession session) {
        Map<String, String> response = new HashMap<>();

        // Auth check
        User user = (User) session.getAttribute("user");
        if (user == null || user.getId() == null) {
            response.put("reply", "Please log in to use the AI assistant.");
            return ResponseEntity.status(401).body(response);
        }

        // Refresh user from DB
        User freshUser = userService.findById(user.getId());
        if (freshUser == null) {
            response.put("reply", "Session expired. Please log in again.");
            return ResponseEntity.status(401).body(response);
        }

        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            response.put("reply", "Please type a message.");
            return ResponseEntity.ok(response);
        }

        String aiReply = chatService.chat(freshUser, userMessage.trim());
        response.put("reply", aiReply);
        return ResponseEntity.ok(response);
    }
}
