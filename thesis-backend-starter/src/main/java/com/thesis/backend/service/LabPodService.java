package com.thesis.backend.service;

import com.thesis.backend.entity.Pod;
import com.thesis.backend.entity.Course;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.PodRepository;
import com.thesis.backend.repository.CourseRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabPodService {
    private final PodRepository podRepository;
    private final CourseRepository courseRepository;
    private final ImageTemplateRepository imageTemplateRepository;
    private final UserRepository userRepository;

    public Pod createPod(Long courseId, Long studentId, Long imageTemplateId, String name) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        User student = userRepository.findById(studentId).orElseThrow();
        ImageTemplate imageTemplate = imageTemplateRepository.findById(imageTemplateId).orElseThrow();

        Pod pod = new Pod();
        pod.setCourse(course);
        pod.setStudent(student);
        pod.setImageTemplate(imageTemplate);
        pod.setName(name);
        pod.setStatus("Creating");
        // Set other fields as needed
        return podRepository.save(pod);
    }

    public List<Pod> getPodsForStudent(User student) {
        return podRepository.findByStudent(student);
    }

    public List<Pod> getPodsForCourse(Course course) {
        return podRepository.findByCourse(course);
    }

    public List<Pod> getAllPods() {
        return podRepository.findAll();
    }
}
