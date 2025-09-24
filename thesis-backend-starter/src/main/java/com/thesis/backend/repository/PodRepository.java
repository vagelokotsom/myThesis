package com.thesis.backend.repository;

import com.thesis.backend.entity.Pod;
import org.springframework.data.jpa.repository.JpaRepository;

import com.thesis.backend.entity.Course;
import com.thesis.backend.entity.User;
import java.util.List;

public interface PodRepository extends JpaRepository<Pod, Long> {
	List<Pod> findByStudent(User student);
	List<Pod> findByCourse(Course course);
}
