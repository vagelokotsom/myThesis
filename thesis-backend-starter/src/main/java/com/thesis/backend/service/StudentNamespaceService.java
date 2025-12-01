package com.thesis.backend.service;

import com.thesis.backend.entity.User;
import io.fabric8.kubernetes.api.model.LimitRange;
import io.fabric8.kubernetes.api.model.LimitRangeBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Opinionated helper that ensures a namespace, quota, and RBAC binding exist for a student.
 * Values are placeholders until we future-proof course-specific tiers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentNamespaceService {

    private final KubernetesClient kubernetesClient;

    @Data
    @Builder
    private static class ResourceTier {
        private final String name;
        private final String pods;
        private final String requestsCpu;
        private final String requestsMemory;
        private final String limitsCpu;
        private final String limitsMemory;
        private final String pvcCount;
        private final String maxPvcSize;
        private final String defaultRequestCpu;
        private final String defaultRequestMemory;
        private final String defaultLimitCpu;
        private final String defaultLimitMemory;
        private final String maxCpu;
        private final String maxMemory;
        private final String minCpu;
        private final String minMemory;
        private final String allowedImages;

        private static ResourceTier resolve(String tierName) {
            // Placeholder: two built-in tiers; in real use, fetch from DB/config
            if ("heavy".equalsIgnoreCase(tierName)) {
                return ResourceTier.builder()
                        .name("heavy")
                        .pods("5")
                        .requestsCpu("4000m")
                        .requestsMemory("4Gi")
                        .limitsCpu("6000m")
                        .limitsMemory("6Gi")
                        .pvcCount("3")
                        .maxPvcSize("5Gi")
                        .defaultRequestCpu("500m")
                        .defaultRequestMemory("512Mi")
                        .defaultLimitCpu("1000m")
                        .defaultLimitMemory("1024Mi")
                        .maxCpu("1500m")
                        .maxMemory("2Gi")
                        .minCpu("100m")
                        .minMemory("128Mi")
                        .allowedImages("thesis-ssh-container:latest,nginx:latest")
                        .build();
            }
            return ResourceTier.builder()
                    .name("standard")
                    .pods("3")
                    .requestsCpu("1500m")
                    .requestsMemory("1Gi")
                    .limitsCpu("2000m")
                    .limitsMemory("2Gi")
                    .pvcCount("2")
                    .maxPvcSize("1Gi")
                    .defaultRequestCpu("250m")
                    .defaultRequestMemory("256Mi")
                    .defaultLimitCpu("500m")
                    .defaultLimitMemory("512Mi")
                    .maxCpu("750m")
                    .maxMemory("768Mi")
                    .minCpu("100m")
                    .minMemory("128Mi")
                    .allowedImages("thesis-ssh-container:latest,nginx:latest")
                    .build();
        }
    }

    public String ensureStudentNamespace(User student, String courseId) {
        return ensureStudentNamespace(student, courseId, "standard", null);
    }

    public String ensureStudentNamespace(User student, String courseId, String tierName, String allowedImages) {
        ResourceTier tier = ResourceTier.resolve(tierName);
        String namespace = (courseId != null && !courseId.isBlank())
                ? String.format("course-%s-student-%s", courseId, student.getId())
                : String.format("student-%s", student.getId());

        Map<String, String> labels = new HashMap<>();
        if (courseId != null && !courseId.isBlank()) {
            labels.put("course", courseId);
        }
        labels.put("student", String.valueOf(student.getId()));
        labels.put("tier", tier.getName());

        Namespace ns = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(namespace)
                    .withLabels(labels)
                    .addToAnnotations("allowed-images", Optional.ofNullable(allowedImages).orElse(tier.getAllowedImages()))
                .endMetadata()
                .build();

        kubernetesClient.namespaces().resource(ns).serverSideApply();
        log.debug("Ensured namespace {}", namespace);

        ResourceQuota quota = new ResourceQuotaBuilder()
                .withNewMetadata()
                    .withName("student-standard-quota")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .addToHard("pods", Quantity.parse(tier.getPods()))
                    .addToHard("requests.cpu", Quantity.parse(tier.getRequestsCpu()))
                    .addToHard("requests.memory", Quantity.parse(tier.getRequestsMemory()))
                    .addToHard("limits.cpu", Quantity.parse(tier.getLimitsCpu()))
                    .addToHard("limits.memory", Quantity.parse(tier.getLimitsMemory()))
                    .addToHard("persistentvolumeclaims", Quantity.parse(tier.getPvcCount()))
                .endSpec()
                .build();
        kubernetesClient.resource(quota).serverSideApply();

        LimitRange limits = new LimitRangeBuilder()
                .withNewMetadata()
                    .withName("student-standard-limits")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .addNewLimit()
                        .withType("Container")
                        .addToDefaultRequest("cpu", Quantity.parse(tier.getDefaultRequestCpu()))
                        .addToDefaultRequest("memory", Quantity.parse(tier.getDefaultRequestMemory()))
                        .addToDefault("cpu", Quantity.parse(tier.getDefaultLimitCpu()))
                        .addToDefault("memory", Quantity.parse(tier.getDefaultLimitMemory()))
                        .addToMax("cpu", Quantity.parse(tier.getMaxCpu()))
                        .addToMax("memory", Quantity.parse(tier.getMaxMemory()))
                        .addToMin("cpu", Quantity.parse(tier.getMinCpu()))
                        .addToMin("memory", Quantity.parse(tier.getMinMemory()))
                    .endLimit()
                    .addNewLimit()
                        .withType("PersistentVolumeClaim")
                        .addToMax("storage", Quantity.parse(tier.getMaxPvcSize()))
                    .endLimit()
                .endSpec()
                .build();
        kubernetesClient.resource(limits).serverSideApply();

        // Per-namespace role for student operations
        Role studentRole = new RoleBuilder()
                .withNewMetadata()
                    .withName("student-workspace-manager")
                    .withNamespace(namespace)
                .endMetadata()
                .addNewRule()
                    .withApiGroups("")
                    .withResources("pods", "pods/log", "services", "persistentvolumeclaims")
                    .withVerbs("get", "list", "watch", "create", "update", "patch", "delete")
                .endRule()
                .build();
        kubernetesClient.resource(studentRole).serverSideApply();

        Subject subject = new Subject();
        subject.setKind("User");
        subject.setName(student.getUsername());

        RoleRef roleRef = new RoleRef();
        roleRef.setKind("Role");
        roleRef.setName("student-workspace-manager");
        roleRef.setApiGroup("rbac.authorization.k8s.io");

        RoleBinding binding = new RoleBindingBuilder()
                .withNewMetadata()
                    .withName("student-workspace-binding")
                    .withNamespace(namespace)
                .endMetadata()
                .withRoleRef(roleRef)
                .withSubjects(subject)
                .build();
        kubernetesClient.resource(binding).serverSideApply();

        return namespace;
    }
}
