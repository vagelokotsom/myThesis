package com.thesis.backend.config;

import com.thesis.backend.entity.Course;
import com.thesis.backend.entity.User;

import com.thesis.backend.entity.Enrollment;
import com.thesis.backend.repository.CourseRepository;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.repository.EnrollmentRepository;
import com.thesis.backend.entity.ContainerTemplate;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.repository.ContainerTemplateRepository;
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
    private final ContainerTemplateRepository containerTemplateRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seed() {
        if (userRepository.count() > 0 || courseRepository.count() > 0 || imageTemplateRepository.count() > 0 || containerTemplateRepository.count() > 0) return;

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
        ImageTemplate ubuntuWorkspace = ImageTemplate.builder()
            .name("Ubuntu SSH Workspace")
            .dockerImage("thesis-ssh-container:latest")
            .description("Ubuntu-based workspace with SSH access, common CLI tools, Python, and Node tooling preinstalled.")
            .persistentStorage(true)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(ubuntuWorkspace);

        ImageTemplate pythonWorkspace = ImageTemplate.builder()
            .name("Python SSH Workspace")
            .dockerImage("thesis-ssh-container:latest")
            .description("Ubuntu SSH workspace ready for Python development; install project requirements after connecting.")
            .persistentStorage(true)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(pythonWorkspace);

        ImageTemplate nodeWorkspace = ImageTemplate.builder()
            .name("Node.js SSH Workspace")
            .dockerImage("thesis-ssh-container:latest")
            .description("Ubuntu SSH workspace prepped for Node.js; npm and npx are available once you connect.")
            .persistentStorage(true)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(nodeWorkspace);

        ImageTemplate nginxWorkspace = ImageTemplate.builder()
            .name("Nginx SSH Workspace")
            .dockerImage("thesis-ssh-container:latest")
            .description("Ubuntu SSH workspace suited for web server labs. Configure Nginx via SSH.")
            .persistentStorage(true)
            .storageSize("1Gi")
            .build();
        imageTemplateRepository.save(nginxWorkspace);

        // Seed a public SSH-enabled container template for student self-provisioning
        ContainerTemplate publicTemplate = ContainerTemplate.builder()
            .name("Student SSH Workspace")
            .description("Public SSH-enabled template for student self-provisioning.")
            .dockerImage("thesis-ssh-container:latest")
            .sshEnabled(true)
            .persistentStorage(true)
            .storageSize("1Gi")
            .isPublic(true)
            .category("Programming")
            .difficultyLevel("Beginner")
            .createdBy(teacher)
            .build();
        containerTemplateRepository.save(publicTemplate);
    }
}
