
package com.thesis.backend.controller;

import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.repository.ImageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageTemplateController {

    private final ImageTemplateRepository repo;

    @GetMapping("/test")
    public String test() {
        System.out.println("DEBUG: Test endpoint called");
        return "Test endpoint works!";
    }

    @GetMapping
    public List<ImageTemplate> all() {
        System.out.println("DEBUG: ImageTemplateController.all() called");
        try {
            List<ImageTemplate> result = repo.findAll();
            System.out.println("DEBUG: Found " + result.size() + " templates");
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in all(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping
    public ImageTemplate create(@RequestBody ImageTemplate template) {
        return repo.save(template);
    }
}
