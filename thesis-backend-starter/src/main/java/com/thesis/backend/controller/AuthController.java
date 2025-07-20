package com.thesis.backend.controller;

import com.thesis.backend.dto.AuthRequest;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            logger.debug("Attempting authentication for user: {}", request.getUsername());
            
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            User user = (User) auth.getPrincipal();
            String token = jwtUtil.generateToken(user);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("role", user.getRole());

            logger.info("Successfully authenticated user: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Authentication failed for user: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        // ... existing register implementation ...
        return ResponseEntity.badRequest().body("Registration is not implemented");
    }

    @GetMapping("/debug/users")
    public ResponseEntity<?> getUsers() {
        List<User> users = userRepository.findAll();
        Map<String, Object> result = new HashMap<>();
        
        for (User user : users) {
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("username", user.getUsername());
            userInfo.put("password_hash", user.getPassword());
            userInfo.put("role", user.getRole());
            result.put(user.getUsername(), userInfo);
        }
        
        return ResponseEntity.ok(result);
    }
}
