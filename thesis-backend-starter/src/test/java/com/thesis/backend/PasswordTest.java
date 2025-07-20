package com.thesis.backend;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {

    @Test
    public void generateCorrectPasswordHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String teacherPassword = "teacher123";
        String studentPassword = "student123";
        
        // Generate new correct hashes
        String teacherHash = encoder.encode(teacherPassword);
        String studentHash = encoder.encode(studentPassword);
        
        System.out.println("=== NEW CORRECT PASSWORD HASHES ===");
        System.out.println("Teacher hash: " + teacherHash);
        System.out.println("Student hash: " + studentHash);
        System.out.println("===================================");
        
        // Verify they work
        assertTrue(encoder.matches(teacherPassword, teacherHash), "Teacher password should match new hash");
        assertTrue(encoder.matches(studentPassword, studentHash), "Student password should match new hash");
        
        // Generate SQL update statements
        System.out.println("\n=== SQL UPDATE COMMANDS ===");
        System.out.println("UPDATE users SET password = '" + teacherHash + "' WHERE username = 'teacher';");
        System.out.println("UPDATE users SET password = '" + studentHash + "' WHERE username = 'student';");
        System.out.println("===========================");
    }
}
