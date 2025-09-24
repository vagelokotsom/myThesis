package com.thesis.backend.config;

import com.thesis.backend.entity.Course;
import com.thesis.backend.entity.User;

import com.thesis.backend.entity.Enrollment;
import com.thesis.backend.repository.CourseRepository;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.repository.EnrollmentRepository;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.repository.ImageTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ImageTemplateRepository imageTemplateRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seed() {
        if (userRepository.count() > 0 || courseRepository.count() > 0 || imageTemplateRepository.count() > 0) return;

        // Create users
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRole("ROLE_ADMIN");
        userRepository.save(admin);

        User teacher = new User();
        teacher.setUsername("teacher1");
        teacher.setEmail("teacher1@example.com");
        teacher.setPassword(passwordEncoder.encode("teacherpass"));
        teacher.setRole("ROLE_TEACHER");
        userRepository.save(teacher);

        User student = new User();
        student.setUsername("student1");
        student.setEmail("student1@example.com");
        student.setPassword(passwordEncoder.encode("studentpass"));
        student.setRole("ROLE_STUDENT");
        userRepository.save(student);

    // Create course and assign teacher
    Course course = new Course();
    course.setName("Kubernetes 101");
    course.setDescription("Intro to Kubernetes");
    course.setTeacher(teacher);
    courseRepository.save(course);

    // Enroll student in course
    Enrollment enrollment = new Enrollment();
    enrollment.setCourse(course);
    enrollment.setStudent(student);
    enrollmentRepository.save(enrollment);
        // Seed demo image templates from Docker Hub
        ImageTemplate ubuntuTemplate = ImageTemplate.builder()
            .name("Ubuntu 22.04")
            .dockerImage("ubuntu:22.04")
            .description("Vanilla Ubuntu 22.04 LTS image from Docker Hub.")
            .persistentStorage(false)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(ubuntuTemplate);

        ImageTemplate nginxTemplate = ImageTemplate.builder()
            .name("Nginx Web Server")
            .dockerImage("nginx:latest")
            .description("Nginx web server image from Docker Hub.")
            .persistentStorage(false)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(nginxTemplate);

        ImageTemplate pythonTemplate = ImageTemplate.builder()
            .name("Python 3.11")
            .dockerImage("python:3.11")
            .description("Python 3.11 base image from Docker Hub.")
            .persistentStorage(false)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(pythonTemplate);

        ImageTemplate nodeTemplate = ImageTemplate.builder()
            .name("Node.js 20")
            .dockerImage("node:20")
            .description("Node.js 20 base image from Docker Hub.")
            .persistentStorage(false)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(nodeTemplate);
    }
}
