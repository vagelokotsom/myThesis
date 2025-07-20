package com.thesis.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordsTest {
    
    @Test
    public void generateCorrectPasswordHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String teacherPassword = "teacher123";
        String studentPassword = "student123";
        
        String teacherHash = encoder.encode(teacherPassword);
        String studentHash = encoder.encode(studentPassword);
        
        System.out.println("=== CORRECT PASSWORD HASHES ===");
        System.out.println("Teacher password hash: " + teacherHash);
        System.out.println("Student password hash: " + studentHash);
        System.out.println("=================================");
        
        // Verify the hashes work
        System.out.println("Teacher hash matches: " + encoder.matches(teacherPassword, teacherHash));
        System.out.println("Student hash matches: " + encoder.matches(studentPassword, studentHash));
        
        // Generate SQL update statements
        System.out.println("\n=== SQL UPDATE STATEMENTS ===");
        System.out.println("UPDATE users SET password = '" + teacherHash + "' WHERE username = 'teacher';");
        System.out.println("UPDATE users SET password = '" + studentHash + "' WHERE username = 'student';");
        System.out.println("===============================");
    }
}
