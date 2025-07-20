package com.thesis.backend.controller;

import com.thesis.backend.entity.User;
import com.thesis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Get all users (for admins and teachers)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            // Create safe user representations without passwords
            List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("role", user.getRole());
                    userInfo.put("status", "active"); // You can add a status field to User entity later
                    return userInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userList);
        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get only students (for teachers)
     */
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllStudents() {
        try {
            List<User> students = userRepository.findByRole("ROLE_STUDENT");
            
            List<Map<String, Object>> studentList = students.stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("role", user.getRole());
                    userInfo.put("status", "active");
                    return userInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(studentList);
        } catch (Exception e) {
            log.error("Failed to fetch students", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user statistics for dashboard
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            List<User> allUsers = userRepository.findAll();
            
            long totalUsers = allUsers.size();
            long students = allUsers.stream()
                .filter(user -> "ROLE_STUDENT".equals(user.getRole()))
                .count();
            long teachers = allUsers.stream()
                .filter(user -> "ROLE_TEACHER".equals(user.getRole()))
                .count();
            long admins = allUsers.stream()
                .filter(user -> "ROLE_ADMIN".equals(user.getRole()))
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalUsers);
            stats.put("students", students);
            stats.put("teachers", teachers);
            stats.put("admins", admins);
            stats.put("active", totalUsers); // Assuming all users are active for now
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch user statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        try {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            userInfo.put("status", "active");
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Failed to fetch current user info", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
