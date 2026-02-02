package com.thesis.backend.controller;

import com.thesis.backend.entity.Course;
import com.thesis.backend.entity.User;
import com.thesis.backend.entity.Enrollment;
import com.thesis.backend.repository.CourseRepository;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @GetMapping
    public ResponseEntity<List<CourseSummary>> getAllCourses(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Course> courses = courseRepository.findAllWithEnrollments();

        if ("ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.ok(courses.stream().map(this::toCourseSummary).toList());
        }

        if ("ROLE_STUDENT".equals(user.getRole())) {
            List<Course> studentCourses = courses.stream()
                    .filter(course -> course.getEnrollments().stream()
                            .anyMatch(enrollment -> enrollment.getStudent() != null
                                    && enrollment.getStudent().getId().equals(user.getId())))
                    .toList();
            return ResponseEntity.ok(studentCourses.stream().map(this::toCourseSummary).toList());
        }

        if ("ROLE_TEACHER".equals(user.getRole())) {
            List<Course> teacherCourses = courses.stream()
                    .filter(course -> course.getTeacher() != null
                            && course.getTeacher().getId().equals(user.getId()))
                    .toList();
            return ResponseEntity.ok(teacherCourses.stream().map(this::toCourseSummary).toList());
        }

        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseRepository.save(course));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course updated) {
        Course course = courseRepository.findById(id).orElseThrow();
        course.setName(updated.getName());
        course.setDescription(updated.getDescription());
        course.setTeacher(updated.getTeacher());
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        courseRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/enroll")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> enrollStudent(@PathVariable Long courseId, @RequestBody EnrollRequest request) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        User student = userRepository.findById(request.getUserId()).orElseThrow();
        // Check if already enrolled
        boolean alreadyEnrolled = course.getEnrollments().stream()
            .anyMatch(e -> e.getStudent().getId().equals(student.getId()));
        if (alreadyEnrolled) {
            return ResponseEntity.badRequest().body("Student already enrolled");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollmentRepository.save(enrollment);
        course.getEnrollments().add(enrollment);
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    @PostMapping("/{courseId}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> assignTeacher(@PathVariable Long courseId, @RequestBody AssignTeacherRequest request) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        User teacher = userRepository.findById(request.getTeacherId()).orElseThrow();
        course.setTeacher(teacher);
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    // DTOs for requests
    public static class EnrollRequest {
        private Long userId;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class AssignTeacherRequest {
        private Long teacherId;
        public Long getTeacherId() { return teacherId; }
        public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    }

    private CourseSummary toCourseSummary(Course course) {
        CourseSummary summary = new CourseSummary();
        summary.setId(course.getId());
        summary.setName(course.getName());
        summary.setDescription(course.getDescription());
        if (course.getTeacher() != null) {
            summary.setTeacher(UserSummary.from(course.getTeacher()));
        }
        summary.setEnrollments(course.getEnrollments().stream()
                .map(enrollment -> EnrollmentSummary.from(enrollment))
                .toList());
        return summary;
    }

    public static class CourseSummary {
        private Long id;
        private String name;
        private String description;
        private UserSummary teacher;
        private List<EnrollmentSummary> enrollments;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public UserSummary getTeacher() { return teacher; }
        public void setTeacher(UserSummary teacher) { this.teacher = teacher; }
        public List<EnrollmentSummary> getEnrollments() { return enrollments; }
        public void setEnrollments(List<EnrollmentSummary> enrollments) { this.enrollments = enrollments; }
    }

    public static class EnrollmentSummary {
        private Long id;
        private UserSummary student;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public UserSummary getStudent() { return student; }
        public void setStudent(UserSummary student) { this.student = student; }

        public static EnrollmentSummary from(Enrollment enrollment) {
            EnrollmentSummary summary = new EnrollmentSummary();
            summary.setId(enrollment.getId());
            if (enrollment.getStudent() != null) {
                summary.setStudent(UserSummary.from(enrollment.getStudent()));
            }
            return summary;
        }
    }

    public static class UserSummary {
        private Long id;
        private String username;
        private String email;
        private String role;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public static UserSummary from(User user) {
            UserSummary summary = new UserSummary();
            summary.setId(user.getId());
            summary.setUsername(user.getUsername());
            summary.setEmail(user.getEmail());
            summary.setRole(user.getRole());
            return summary;
        }
    }
}
