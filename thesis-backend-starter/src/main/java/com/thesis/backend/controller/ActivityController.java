package com.thesis.backend.controller;

import com.thesis.backend.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    /**
     * Get recent activities (for teachers and admins)
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivities() {
        try {
            // TODO: Implement proper activity tracking with database entities
            // For now, return mock data to make the frontend work
            List<Map<String, Object>> activities = generateMockActivities();
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            log.error("Failed to fetch recent activities", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user's own activities (for students)
     */
    @GetMapping("/my-activities")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMyActivities(@AuthenticationPrincipal User user) {
        try {
            // TODO: Implement proper activity tracking filtered by user
            // For now, return mock data specific to the user
            List<Map<String, Object>> activities = generateMockUserActivities(user);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            log.error("Failed to fetch user activities for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Log a new activity (internal use)
     */
    @PostMapping("/log")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> logActivity(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> activityData) {
        try {
            // TODO: Implement activity logging to database
            log.info("Activity logged for user {}: {}", user.getUsername(), activityData);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Activity logged successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to log activity for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate mock activities for demonstration
     */
    private List<Map<String, Object>> generateMockActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Mock recent activities
        activities.add(createActivity("Container Created", "student", "Created new Python container", 
                LocalDateTime.now().minusMinutes(5).format(formatter), "container"));
        activities.add(createActivity("SSH Connection", "student", "Connected to container via SSH", 
                LocalDateTime.now().minusMinutes(15).format(formatter), "ssh"));
        activities.add(createActivity("Template Created", "teacher", "Created new Data Science template", 
                LocalDateTime.now().minusMinutes(30).format(formatter), "template"));
        activities.add(createActivity("Container Started", "student", "Started existing container", 
                LocalDateTime.now().minusHours(1).format(formatter), "container"));
        activities.add(createActivity("User Login", "teacher", "Logged into the system", 
                LocalDateTime.now().minusHours(2).format(formatter), "auth"));
        
        return activities;
    }

    /**
     * Generate mock user-specific activities
     */
    private List<Map<String, Object>> generateMockUserActivities(User user) {
        List<Map<String, Object>> activities = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        if ("ROLE_STUDENT".equals(user.getRole())) {
            activities.add(createActivity("Container Access", user.getUsername(), "Accessed Python container", 
                    LocalDateTime.now().minusMinutes(10).format(formatter), "container"));
            activities.add(createActivity("SSH Session", user.getUsername(), "Started SSH session", 
                    LocalDateTime.now().minusMinutes(25).format(formatter), "ssh"));
            activities.add(createActivity("Login", user.getUsername(), "Logged into the system", 
                    LocalDateTime.now().minusHours(1).format(formatter), "auth"));
        } else if ("ROLE_TEACHER".equals(user.getRole())) {
            activities.add(createActivity("Dashboard View", user.getUsername(), "Viewed student progress", 
                    LocalDateTime.now().minusMinutes(5).format(formatter), "dashboard"));
            activities.add(createActivity("Template Update", user.getUsername(), "Updated container template", 
                    LocalDateTime.now().minusMinutes(20).format(formatter), "template"));
            activities.add(createActivity("Container Review", user.getUsername(), "Reviewed student containers", 
                    LocalDateTime.now().minusMinutes(45).format(formatter), "container"));
        }
        
        return activities;
    }

    /**
     * Helper method to create activity object
     */
    private Map<String, Object> createActivity(String action, String user, String description, 
                                               String timestamp, String type) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("id", UUID.randomUUID().toString());
        activity.put("action", action);
        activity.put("user", user);
        activity.put("description", description);
        activity.put("timestamp", timestamp);
        activity.put("type", type);
        return activity;
    }
}
