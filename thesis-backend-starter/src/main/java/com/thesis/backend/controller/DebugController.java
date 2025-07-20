package com.thesis.backend.controller;

import com.thesis.backend.entity.User;
import com.thesis.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            // Create a safe representation without exposing full passwords
            List<Object> userData = users.stream().map(user -> {
                return new Object() {
                    public final String username = user.getUsername();
                    public final String email = user.getEmail(); 
                    public final String role = user.getRole();
                    public final String passwordStart = user.getPassword() != null ? 
                        user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "null";
                    public final int passwordLength = user.getPassword() != null ? user.getPassword().length() : 0;
                };
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
