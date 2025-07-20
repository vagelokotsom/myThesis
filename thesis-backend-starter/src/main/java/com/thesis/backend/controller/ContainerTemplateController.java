package com.thesis.backend.controller;

import com.thesis.backend.entity.ContainerTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/container-templates")
@RequiredArgsConstructor
public class ContainerTemplateController {
    
    private final ContainerTemplateRepository containerTemplateRepository;
    
    /**
     * Get all available container templates
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<List<ContainerTemplate>> getAvailableTemplates(@AuthenticationPrincipal User user) {
        List<ContainerTemplate> templates = containerTemplateRepository.findAvailableTemplates(user);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Get SSH-enabled templates only
     */
    @GetMapping("/ssh-enabled")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<List<ContainerTemplate>> getSshEnabledTemplates(@AuthenticationPrincipal User user) {
        List<ContainerTemplate> templates = containerTemplateRepository.findSshEnabledTemplates(user);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Get templates by category
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<List<ContainerTemplate>> getTemplatesByCategory(@PathVariable String category) {
        List<ContainerTemplate> templates = containerTemplateRepository.findByCategory(category);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Create a new container template (teachers only)
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ContainerTemplate> createTemplate(
            @RequestBody ContainerTemplate template,
            @AuthenticationPrincipal User teacher) {
        
        template.setCreatedBy(teacher);
        ContainerTemplate savedTemplate = containerTemplateRepository.save(template);
        log.info("Created container template: {} by teacher: {}", template.getName(), teacher.getUsername());
        return ResponseEntity.ok(savedTemplate);
    }
    
    /**
     * Update container template
     */
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ContainerTemplate> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody ContainerTemplate template,
            @AuthenticationPrincipal User teacher) {
        
        ContainerTemplate existingTemplate = containerTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        // Check if teacher owns this template
        if (!existingTemplate.getCreatedBy().getId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        template.setId(templateId);
        template.setCreatedBy(teacher);
        ContainerTemplate savedTemplate = containerTemplateRepository.save(template);
        return ResponseEntity.ok(savedTemplate);
    }
    
    /**
     * Delete container template
     */
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, String>> deleteTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal User teacher) {
        
        ContainerTemplate template = containerTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        if (!template.getCreatedBy().getId().equals(teacher.getId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Access denied");
            return ResponseEntity.status(403).body(error);
        }
        
        containerTemplateRepository.delete(template);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Template deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get teacher's own templates
     */
    @GetMapping("/my-templates")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<ContainerTemplate>> getMyTemplates(@AuthenticationPrincipal User teacher) {
        List<ContainerTemplate> templates = containerTemplateRepository.findByCreatedBy(teacher);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Search templates by name
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<List<ContainerTemplate>> searchTemplates(@RequestParam String name) {
        List<ContainerTemplate> templates = containerTemplateRepository.findByNameContainingIgnoreCase(name);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Get predefined templates with example configurations
     */
    @GetMapping("/examples")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getExampleTemplates() {
        // Return predefined template examples that teachers can use as starting points
        // This would be useful for quick setup
        return ResponseEntity.ok(getPredefinedTemplates());
    }
    
    private List<Map<String, Object>> getPredefinedTemplates() {
        return List.of(
            Map.of(
                "name", "Python Development Environment",
                "description", "Ubuntu container with Python 3, pip, and common data science libraries",
                "dockerImage", "python:3.9-slim",
                "category", "Programming",
                "difficultyLevel", "Beginner",
                "preInstalledTools", List.of("python3", "pip", "numpy", "pandas", "matplotlib"),
                "sshEnabled", true
            ),
            Map.of(
                "name", "Node.js Development Environment", 
                "description", "Ubuntu container with Node.js, npm, and common web development tools",
                "dockerImage", "node:16-alpine",
                "category", "Web Development",
                "difficultyLevel", "Intermediate",
                "preInstalledTools", List.of("nodejs", "npm", "git", "curl"),
                "sshEnabled", true
            ),
            Map.of(
                "name", "Java Development Environment",
                "description", "Ubuntu container with OpenJDK, Maven, and development tools",
                "dockerImage", "openjdk:11-jdk-slim",
                "category", "Programming", 
                "difficultyLevel", "Intermediate",
                "preInstalledTools", List.of("openjdk-11", "maven", "git"),
                "sshEnabled", true
            ),
            Map.of(
                "name", "Data Science Environment",
                "description", "Jupyter notebook environment with Python, R, and data science libraries",
                "dockerImage", "jupyter/datascience-notebook:latest",
                "category", "Data Science",
                "difficultyLevel", "Advanced",
                "preInstalledTools", List.of("jupyter", "python3", "r", "pandas", "scikit-learn", "tensorflow"),
                "sshEnabled", true
            )
        );
    }
}
