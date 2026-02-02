package com.thesis.backend.service;

import com.thesis.backend.entity.Course;
import com.thesis.backend.entity.Enrollment;
import com.thesis.backend.entity.User;
import com.thesis.backend.model.KubernetesNamespace;
import com.thesis.backend.repository.CourseRepository;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NamespaceService {

    @Autowired
    private KubernetesClient kubernetesClient;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Get all namespaces
     */
    public List<KubernetesNamespace> getAllNamespaces() {
        return kubernetesClient.namespaces().list().getItems()
                .stream().map(this::mapNamespaceToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get namespaces visible to a specific user.
     */
    public List<KubernetesNamespace> getNamespacesForUser(User user) {
        if (isAdmin(user)) {
            return getAllNamespaces();
        }

        List<Namespace> namespaces = kubernetesClient.namespaces().list().getItems();
        List<String> allowedNames = resolveAllowedNamespaceNames(user);
        if (allowedNames.isEmpty()) {
            return List.of();
        }

        return namespaces.stream()
                .filter(ns -> allowedNames.contains(ns.getMetadata().getName()))
                .map(this::mapNamespaceToDto)
                .collect(Collectors.toList());
    }

    /**
     * Check if a namespace is allowed for the given user.
     */
    public boolean isNamespaceAllowedForUser(User user, String namespaceName) {
        if (isAdmin(user)) {
            return true;
        }
        return resolveAllowedNamespaceNames(user).contains(namespaceName);
    }

    /**
     * Get namespace by name
     */
    public KubernetesNamespace getNamespace(String name) {
        Namespace namespace = kubernetesClient.namespaces().withName(name).get();
        if (namespace == null) {
            return null;
        }
        return mapNamespaceToDto(namespace);
    }

    /**
     * Create a new namespace
     */
    public KubernetesNamespace createNamespace(String name, Map<String, String> labels) {
        Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withLabels(labels)
                .endMetadata()
                .build();

    Namespace createdNamespace = kubernetesClient.namespaces().resource(namespace).serverSideApply();
        return mapNamespaceToDto(createdNamespace);
    }

    /**
     * Delete a namespace
     */
    public boolean deleteNamespace(String name) {
        return !kubernetesClient.namespaces().withName(name).delete().isEmpty();
    }

    /**
     * Map Kubernetes Namespace to a simplified DTO
     */
    private KubernetesNamespace mapNamespaceToDto(Namespace namespace) {
        KubernetesNamespace kubernetesNamespace = new KubernetesNamespace();
        kubernetesNamespace.setName(namespace.getMetadata().getName());
        kubernetesNamespace.setStatus(
                namespace.getStatus() != null ? namespace.getStatus().getPhase() : null);
        
        if (namespace.getMetadata().getLabels() != null) {
            kubernetesNamespace.setLabels(namespace.getMetadata().getLabels());
        }
        
        if (namespace.getMetadata().getCreationTimestamp() != null) {
            kubernetesNamespace.setCreationTimestamp(
                    namespace.getMetadata().getCreationTimestamp());
        }
        
        return kubernetesNamespace;
    }

    private boolean isAdmin(User user) {
        return user != null && "ROLE_ADMIN".equals(user.getRole());
    }

    private List<String> resolveAllowedNamespaceNames(User user) {
        if (user == null) {
            return List.of();
        }

        if ("ROLE_STUDENT".equals(user.getRole())) {
            return buildStudentNamespaceNames(user.getId(), getStudentCourseIds(user.getId()));
        }

        if ("ROLE_TEACHER".equals(user.getRole())) {
            List<Course> courses = courseRepository.findAll().stream()
                    .filter(course -> course.getTeacher() != null
                            && course.getTeacher().getId().equals(user.getId()))
                    .collect(Collectors.toList());

            List<Long> studentIds = courses.stream()
                    .flatMap(course -> course.getEnrollments().stream())
                    .map(Enrollment::getStudent)
                    .filter(student -> student != null)
                    .map(User::getId)
                    .distinct()
                    .collect(Collectors.toList());

            return studentIds.stream()
                    .flatMap(studentId -> buildStudentNamespaceNames(studentId, courseIdsForStudent(courses, studentId)).stream())
                    .distinct()
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private List<Long> getStudentCourseIds(Long studentId) {
        return courseRepository.findAll().stream()
                .flatMap(course -> course.getEnrollments().stream()
                        .filter(enrollment -> enrollment.getStudent() != null
                                && enrollment.getStudent().getId().equals(studentId))
                        .map(enrollment -> course.getId()))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> courseIdsForStudent(List<Course> courses, Long studentId) {
        return courses.stream()
                .flatMap(course -> course.getEnrollments().stream()
                        .filter(enrollment -> enrollment.getStudent() != null
                                && enrollment.getStudent().getId().equals(studentId))
                        .map(enrollment -> course.getId()))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> buildStudentNamespaceNames(Long studentId, List<Long> courseIds) {
        List<String> names = new java.util.ArrayList<>();
        names.add(String.format("student-%d", studentId));
        if (courseIds != null) {
            courseIds.forEach(courseId ->
                    names.add(String.format("course-%d-student-%d", courseId, studentId)));
        }
        return names;
    }
}
