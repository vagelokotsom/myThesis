package com.thesis.backend.controller;

import com.thesis.backend.entity.User;
import com.thesis.backend.entity.Course;
import com.thesis.backend.repository.CourseRepository;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

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
    public ResponseEntity<List<Map<String, Object>>> getAllStudents(@AuthenticationPrincipal User currentUser) {
        try {
            List<User> students;
            if (currentUser != null && "ROLE_ADMIN".equals(currentUser.getRole())) {
                students = userRepository.findByRole("ROLE_STUDENT");
            } else if (currentUser != null && "ROLE_TEACHER".equals(currentUser.getRole())) {
                List<Course> courses = courseRepository.findAll().stream()
                    .filter(course -> course.getTeacher() != null
                        && course.getTeacher().getId().equals(currentUser.getId()))
                    .toList();

                List<Long> allowedStudentIds = courses.stream()
                    .flatMap(course -> course.getEnrollments().stream())
                    .map(enrollment -> enrollment.getStudent())
                    .filter(student -> student != null)
                    .map(User::getId)
                    .distinct()
                    .toList();

                if (allowedStudentIds.isEmpty()) {
                    return ResponseEntity.ok(List.of());
                }

                students = userRepository.findAllById(allowedStudentIds).stream()
                    .filter(user -> "ROLE_STUDENT".equals(user.getRole()))
                    .toList();
            } else {
                students = List.of();
            }
            
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
     * Search students with pagination for large datasets.
     */
    @GetMapping("/students/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> searchStudents(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            int safePage = Math.max(0, page);
            int safeSize = Math.min(Math.max(1, size), 50);

            if (currentUser != null && "ROLE_ADMIN".equals(currentUser.getRole())) {
                Page<User> result = userRepository.searchByRole("ROLE_STUDENT", query, PageRequest.of(safePage, safeSize));
                return ResponseEntity.ok(buildPagedResponse(result));
            }

            if (currentUser != null && "ROLE_TEACHER".equals(currentUser.getRole())) {
                List<Course> courses = courseRepository.findAll().stream()
                    .filter(course -> course.getTeacher() != null
                        && course.getTeacher().getId().equals(currentUser.getId()))
                    .toList();

                List<User> allowedStudents = courses.stream()
                    .flatMap(course -> course.getEnrollments().stream())
                    .map(enrollment -> enrollment.getStudent())
                    .filter(student -> student != null && "ROLE_STUDENT".equals(student.getRole()))
                    .distinct()
                    .toList();

                String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
                List<User> filtered = allowedStudents.stream()
                    .filter(student -> normalizedQuery.isBlank()
                        || student.getUsername().toLowerCase().contains(normalizedQuery)
                        || student.getEmail().toLowerCase().contains(normalizedQuery))
                    .toList();

                int from = Math.min(safePage * safeSize, filtered.size());
                int to = Math.min(from + safeSize, filtered.size());
                List<User> pageItems = filtered.subList(from, to);

                Map<String, Object> response = new HashMap<>();
                response.put("items", pageItems.stream().map(this::toUserInfo).toList());
                response.put("page", safePage);
                response.put("size", safeSize);
                response.put("total", filtered.size());
                response.put("hasMore", to < filtered.size());
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.ok(Map.of(
                "items", List.of(),
                "page", 0,
                "size", safeSize,
                "total", 0,
                "hasMore", false
            ));
        } catch (Exception e) {
            log.error("Failed to search students", e);
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
            userInfo.put("sshPublicKeySet", user.getSshPublicKey() != null && !user.getSshPublicKey().isBlank());
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Failed to fetch current user info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> buildPagedResponse(Page<User> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("items", page.getContent().stream().map(this::toUserInfo).toList());
        response.put("page", page.getNumber());
        response.put("size", page.getSize());
        response.put("total", page.getTotalElements());
        response.put("hasMore", page.getNumber() + 1 < page.getTotalPages());
        return response;
    }

    private Map<String, Object> toUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        userInfo.put("status", "active");
        return userInfo;
    }

    /**
     * Set or replace the current user's SSH public key.
     */
    @PutMapping("/me/ssh-key")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<?> updateSshPublicKey(@AuthenticationPrincipal User user,
                                                @RequestBody Map<String, String> payload) {
        try {
            String publicKey = payload.get("publicKey");
            if (publicKey == null || publicKey.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "publicKey is required"));
            }

            user.setSshPublicKey(publicKey.trim());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "SSH public key saved"));
        } catch (Exception e) {
            log.error("Failed to update SSH public key for user {}", user.getUsername(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update SSH key"));
        }
    }

    /**
     * Remove the current user's SSH public key.
     */
    @DeleteMapping("/me/ssh-key")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<?> deleteSshPublicKey(@AuthenticationPrincipal User user) {
        try {
            user.setSshPublicKey(null);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "SSH public key removed"));
        } catch (Exception e) {
            log.error("Failed to delete SSH public key for user {}", user.getUsername(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete SSH key"));
        }
    }
}
