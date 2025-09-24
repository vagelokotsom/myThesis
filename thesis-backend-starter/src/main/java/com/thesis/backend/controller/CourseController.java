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
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
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
}
