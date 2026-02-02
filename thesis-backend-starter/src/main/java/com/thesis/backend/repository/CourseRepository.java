package com.thesis.backend.repository;

import com.thesis.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("""
        select distinct c from Course c
        left join fetch c.teacher
        left join fetch c.enrollments e
        left join fetch e.student
        """)
    List<Course> findAllWithEnrollments();
}
