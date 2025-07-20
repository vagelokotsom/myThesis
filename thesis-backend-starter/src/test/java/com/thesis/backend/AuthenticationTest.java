package com.thesis.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.thesis.backend.entity.User;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.service.UserDetailsServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuthenticationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired 
    private UserDetailsServiceImpl userDetailsService;
    
    @Test
    public void testInMemoryUserAuthentication() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Create user with known hash in memory for testing
        User testUser = new User();
        testUser.setUsername("testteacher");
        testUser.setPassword("$2a$10$QLcHn3PWgcHq6aT7w3lKtOvZab6TDznqSNTW8nHZKIRWNqZtjhF26"); // teacher123
        testUser.setEmail("test@example.com");
        testUser.setRole("ROLE_TEACHER");
        
        // Save to repository
        userRepository.save(testUser);
        
        // Test password matching
        String rawPassword = "teacher123";
        boolean matches = encoder.matches(rawPassword, testUser.getPassword());
        
        System.out.println("=== IN-MEMORY AUTHENTICATION TEST ===");
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Stored hash: " + testUser.getPassword());
        System.out.println("Password matches: " + matches);
        
        assertTrue(matches, "Password should match the hash");
        
        // Test UserDetailsService
        try {
            User loadedUser = (User) userDetailsService.loadUserByUsername("testteacher");
            assertNotNull(loadedUser);
            assertEquals("testteacher", loadedUser.getUsername());
            System.out.println("UserDetailsService loading: SUCCESS");
        } catch (Exception e) {
            System.out.println("UserDetailsService loading failed: " + e.getMessage());
            fail("UserDetailsService should load the user");
        }
        
        System.out.println("=== TEST COMPLETED ===");
    }
}
