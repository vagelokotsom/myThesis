
package com.thesis.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ContainerTemplate;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ContainerTemplateRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.repository.UserRepository;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerInstanceService {
    
    private final ContainerInstanceRepository containerInstanceRepository;
    private final ContainerTemplateRepository containerTemplateRepository;
    private final ImageTemplateRepository imageTemplateRepository;
    private final UserRepository userRepository;
    private final KubernetesClient kubernetesClient;
    private final StudentNamespaceService studentNamespaceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${ssh.container.namespace:default}")
    private String namespace;

    @Value("${ssh.container.image:thesis-ssh-container:latest}")
    private String sshImageName;

    @Value("${ssh.container.password.student:student123}")
    private String studentSshPassword;

    /**
     * Create a container instance from a template for a specific student entity
     */
    public ContainerInstance createContainerFromTemplate(Long templateId, User student, User teacher, String tierName) {
        return createContainerFromTemplateInternal(templateId, student, teacher, tierName);
    }

    /**
     * Create a container instance from a template for a student by ID (teacher/admin flow)
     */
    public ContainerInstance createContainerFromTemplate(Long templateId, Long studentId, User actor, String tierName) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new RuntimeException("User is not a student");
        }

        return createContainerFromTemplateInternal(templateId, student, actor, tierName);
    }
    
    /**
     * Create a container instance from an image template for a student (by IDs)
     */
    public ContainerInstance createContainerForStudent(Long imageId, Long studentId, User actor, String tierName) {
        // Find the student
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        // Verify the user is actually a student
        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new RuntimeException("User is not a student");
        }

        // Find the image template
        ImageTemplate imageTemplate = imageTemplateRepository.findById(imageId)
            .orElseThrow(() -> new RuntimeException("Image template not found with id: " + imageId));

        // Ensure namespace for the student exists (course support TBD)
        String targetNamespace = studentNamespaceService.ensureStudentNamespace(student, null, tierName, null);

        // Generate unique name for the container
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String containerName = String.format("container-%s-%s",
            student.getUsername().toLowerCase(),
            timestamp);

        // Unified pod creation logic
        String podName = createUnifiedKubernetesPod(
            containerName,
            imageTemplate.getDockerImage(),
            student,
            imageTemplate.getResourceLimits(),
            imageTemplate.getEnvironmentVars(),
            imageTemplate.getPersistentStorage(),
            imageTemplate.getStorageSize(),
            true, // SSH enabled for image templates
            targetNamespace
        );

        // Create container instance record
        ContainerInstance instance = ContainerInstance.builder()
            .name(containerName)
            .status("Creating")
            .kubernetesPodName(podName)
            .kubernetesNamespace(targetNamespace)
            .owner(student)
            .imageTemplate(imageTemplate)
            .build();

        ContainerInstance savedInstance = containerInstanceRepository.save(instance);

        // Update status after pod creation
        updateContainerStatus(savedInstance);

        String actorName = actor != null ? actor.getUsername() : "system";
        log.info("Created container instance {} for student {} using image template {} by {}",
            containerName, student.getUsername(), imageTemplate.getName(), actorName);

        return savedInstance;
    }

    /**
     * Shared logic for container template provisioning
     */
    private ContainerInstance createContainerFromTemplateInternal(Long templateId, User student, User actor, String tierName) {
        ContainerTemplate template = containerTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Container template not found with id: " + templateId));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String templateSlug = template.getName() != null
                ? template.getName().toLowerCase().replaceAll("[^a-z0-9]", "-")
                : "template";
        String containerName = String.format("%s-%s-%s",
                templateSlug,
                student.getUsername().toLowerCase(),
                timestamp);

        Map<String, String> resourceLimits = parseResourceLimits(template.getResourceLimits());
        Map<String, String> environmentVars = parseEnvironmentVariables(template.getEnvironmentVars());
        boolean persistentStorage = Boolean.TRUE.equals(template.getPersistentStorage());
        String storageSize = template.getStorageSize();
        Boolean sshEnabled = template.getSshEnabled() != null ? template.getSshEnabled() : Boolean.TRUE;

        String targetNamespace = studentNamespaceService.ensureStudentNamespace(student, null, tierName, null);

        String podName = createUnifiedKubernetesPod(
                containerName,
                template.getDockerImage(),
                student,
                resourceLimits,
                environmentVars,
                persistentStorage,
                storageSize,
                sshEnabled,
                targetNamespace
        );

        ContainerInstance instance = ContainerInstance.builder()
                .name(containerName)
                .status("Creating")
                .kubernetesPodName(podName)
                .kubernetesNamespace(targetNamespace)
                .owner(student)
                .imageTemplate(null)
                .containerTemplate(template)
                .build();

        ContainerInstance savedInstance = containerInstanceRepository.save(instance);

        updateContainerStatus(savedInstance);

        String actorName = actor != null ? actor.getUsername() : "system";
        log.info("Created container instance {} for student {} using container template {} by {}",
                containerName, student.getUsername(), template.getName(), actorName);

        return savedInstance;
    }

    private Map<String, String> parseResourceLimits(String raw) {
        Map<String, String> parsed = parseJsonStringToMap(raw, "resource limits");
        return parsed != null ? parsed : new HashMap<>();
    }

    private Map<String, String> parseEnvironmentVariables(String raw) {
        Map<String, String> parsed = parseJsonStringToMap(raw, "environment variables");
        if (parsed != null) {
            return parsed;
        }

        Map<String, String> fallback = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || !trimmed.contains("=")) {
                continue;
            }
            String[] parts = trimmed.split("=", 2);
            fallback.put(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
        }
        return fallback;
    }

    private Map<String, String> parseJsonStringToMap(String raw, String context) {
        if (raw == null || raw.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse {} JSON: {}", context, e.getMessage());
            return null;
        }
    }

    /**
     * Unified pod creation logic for both ImageTemplate and ContainerTemplate
     */
    private String createUnifiedKubernetesPod(
            String containerName,
            String dockerImage,
            User student,
            Map<String, String> resourceLimits,
            Map<String, String> environmentVars,
            Boolean persistentStorage,
            String storageSize,
            Boolean sshEnabled,
            String targetNamespace
    ) {
        String effectiveNamespace = resolveNamespace(targetNamespace);
        boolean sshEnabledFlag = sshEnabled != null && sshEnabled;
        Map<String, String> labels = new HashMap<>();
        labels.put("app", containerName);
        labels.put("owner", student.getUsername());
        labels.put("type", "student-container");
        labels.put("ssh-enabled", sshEnabledFlag ? "true" : "false");

        // Build container
        String resolvedImage = (dockerImage != null && !dockerImage.isBlank()) ? dockerImage : sshImageName;

        ContainerBuilder mainContainer = new ContainerBuilder()
            .withName("main-container")
            .withImage(resolvedImage)
            .withImagePullPolicy("IfNotPresent");

        // Keep main container alive if it has no long-running process (non-SSH images)
        if (!sshEnabledFlag) {
            mainContainer = mainContainer.withCommand(Arrays.asList("/bin/sh", "-c", "tail -f /dev/null"));
        }


        // Add environment variables
        if (environmentVars != null && !environmentVars.isEmpty()) {
            for (Map.Entry<String, String> entry : environmentVars.entrySet()) {
                mainContainer = mainContainer
                        .addNewEnv()
                            .withName(entry.getKey())
                            .withValue(entry.getValue())
                        .endEnv();
            }
        }
        // Always add workspace user
        mainContainer = mainContainer
                .addNewEnv()
                    .withName("WORKSPACE_USER")
                    .withValue(student.getUsername())
                .endEnv();

        // Add resource limits
        if (resourceLimits != null && !resourceLimits.isEmpty()) {
            ContainerBuilder.ResourcesNested<ContainerBuilder> resourcesBuilder = mainContainer.withNewResources();
            if (resourceLimits.containsKey("memory-request")) {
                resourcesBuilder = resourcesBuilder.addToRequests("memory", new Quantity(resourceLimits.get("memory-request")));
            }
            if (resourceLimits.containsKey("cpu-request")) {
                resourcesBuilder = resourcesBuilder.addToRequests("cpu", new Quantity(resourceLimits.get("cpu-request")));
            }
            if (resourceLimits.containsKey("memory-limit")) {
                resourcesBuilder = resourcesBuilder.addToLimits("memory", new Quantity(resourceLimits.get("memory-limit")));
            }
            if (resourceLimits.containsKey("cpu-limit")) {
                resourcesBuilder = resourcesBuilder.addToLimits("cpu", new Quantity(resourceLimits.get("cpu-limit")));
            }
            mainContainer = resourcesBuilder.endResources();
        }

        // Add persistent volume mount if required
        if (persistentStorage != null && persistentStorage) {
            mainContainer = mainContainer
                    .addNewVolumeMount()
                        .withName("workspace-storage")
                        .withMountPath("/workspace")
                    .endVolumeMount();
        }

        // Build the pod spec
        // For SSH-enabled templates, expose SSH on the main container; otherwise attach sidecar
        PodSpecBuilder podSpecBuilder = new PodSpecBuilder();
        if (sshEnabledFlag) {
            mainContainer = mainContainer
                    .addNewPort()
                        .withContainerPort(22)
                        .withProtocol("TCP")
                        .withName("ssh")
                    .endPort()
                    .addNewEnv()
                        .withName("ROOT_PASSWORD")
                        .withValue(studentSshPassword)
                    .endEnv()
                    .addNewEnv()
                        .withName("SSH_ENABLED")
                        .withValue("true")
                    .endEnv();
            podSpecBuilder = podSpecBuilder.addToContainers(mainContainer.build());
        } else {
            int sidecarSshPort = 2222;
            String sidecarCmd = String.format("echo \"root:%s\" | chpasswd && /usr/sbin/sshd -D -p %d -o PermitRootLogin=yes -o PasswordAuthentication=yes",
                    studentSshPassword, sidecarSshPort);
            ContainerBuilder sshSidecar = new ContainerBuilder()
                    .withName("ssh-sidecar")
                    .withImage(sshImageName)
                    .withImagePullPolicy("IfNotPresent")
                    .withCommand(Arrays.asList("/bin/sh", "-c", sidecarCmd))
                    .addNewPort()
                        .withContainerPort(sidecarSshPort)
                        .withProtocol("TCP")
                        .withName("ssh")
                    .endPort()
                    .addNewEnv()
                        .withName("ROOT_PASSWORD")
                        .withValue(studentSshPassword)
                    .endEnv()
                    .addNewEnv()
                        .withName("SSH_ENABLED")
                        .withValue("true")
                    .endEnv();

            if (persistentStorage != null && persistentStorage) {
                sshSidecar = sshSidecar
                        .addNewVolumeMount()
                            .withName("workspace-storage")
                            .withMountPath("/workspace")
                        .endVolumeMount();
            }
            podSpecBuilder = podSpecBuilder.addToContainers(mainContainer.build(), sshSidecar.build());
        }
        podSpecBuilder = podSpecBuilder.withRestartPolicy("Always");

        // Add persistent volume if required
        if (persistentStorage != null && persistentStorage) {
            podSpecBuilder = podSpecBuilder
                .addNewVolume()
                    .withName("workspace-storage")
                    .withNewPersistentVolumeClaim()
                        .withClaimName(containerName + "-pvc")
                    .endPersistentVolumeClaim()
                .endVolume();
        }

        // Build the complete pod
        Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName(containerName)
                    .withNamespace(effectiveNamespace)
                    .withLabels(labels)
                .endMetadata()
                .withSpec(podSpecBuilder.build())
                .build();

        // Create PVC if persistent storage is required
        if (persistentStorage != null && persistentStorage) {
            createPersistentVolumeClaim(containerName, storageSize, effectiveNamespace);
        }

        // Create the pod
        kubernetesClient.pods().inNamespace(effectiveNamespace).resource(pod).create();

        // Create NodePort service for SSH access if enabled
        if (sshEnabled != null && sshEnabled) {
            createNodePortService(containerName, labels, effectiveNamespace, new IntOrString(22));
        } else {
            createNodePortService(containerName, labels, effectiveNamespace, new IntOrString(2222));
        }

        log.info("Created unified Kubernetes pod {} for student {}", containerName, student.getUsername());
        return containerName;
    }
    

    /**
     * Get all containers for a student
     */
    public List<ContainerInstance> getStudentContainers(User student) {
        return containerInstanceRepository.findByOwner(student);
    }
    
    /**
     * Get all containers (for teachers)
     */
    public List<ContainerInstance> getAllContainers() {
        return containerInstanceRepository.findAll();
    }
    
    /**
     * Stop a container
     */
    public void stopContainer(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        String effectiveNamespace = resolveNamespace(instance.getKubernetesNamespace());
        
        // Check authorization
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete the Kubernetes pod and service
        try {
            kubernetesClient.pods().inNamespace(effectiveNamespace).withName(instance.getKubernetesPodName()).delete();
            kubernetesClient.services().inNamespace(effectiveNamespace).withName(instance.getKubernetesPodName() + "-ssh").delete();
            
            log.info("Stopped Kubernetes pod and service for container {}", instance.getName());
        } catch (Exception e) {
            log.warn("Could not stop Kubernetes resources (development mode): {}", e.getMessage());
        }
        
        // Update status
        instance.setStatus("Stopped");
        containerInstanceRepository.save(instance);
        
        log.info("Stopped container {} by user {}", instance.getName(), user.getUsername());
    }
    
    /**
     * Start a stopped container
     */
    public void startContainer(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        String effectiveNamespace = resolveNamespace(instance.getKubernetesNamespace());
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // Only recreate Kubernetes resources for containers created from image templates for now
        ImageTemplate template = instance.getImageTemplate();
        if (template == null) {
            throw new RuntimeException("Restart is currently supported only for image-template based containers. Please recreate the container from its template.");
        }

        String containerName = instance.getKubernetesPodName();
        if (containerName == null || containerName.isBlank()) {
            containerName = instance.getName();
            instance.setKubernetesPodName(containerName);
        }

        // Clean up any lingering Kubernetes resources just to be safe
        try {
            kubernetesClient.pods().inNamespace(effectiveNamespace).withName(containerName).delete();
        } catch (Exception e) {
            log.debug("Unable to delete existing pod {} during restart: {}", containerName, e.getMessage());
        }
        try {
            kubernetesClient.services().inNamespace(effectiveNamespace).withName(containerName + "-ssh").delete();
        } catch (Exception e) {
            log.debug("Unable to delete existing service {}-ssh during restart: {}", containerName, e.getMessage());
        }

        instance.setStatus("Starting");
        containerInstanceRepository.save(instance);

        createUnifiedKubernetesPod(
                containerName,
                template.getDockerImage(),
                instance.getOwner(),
                template.getResourceLimits(),
                template.getEnvironmentVars(),
                template.getPersistentStorage(),
                template.getStorageSize(),
                true,
                effectiveNamespace
        );

        // Update status asynchronously based on the real pod state
        updateContainerStatus(instance);

        log.info("Restarted container {} for user {}", instance.getName(), user.getUsername());
    }
    
    /**
     * Delete a container permanently
     */
    public void deleteContainer(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        String effectiveNamespace = resolveNamespace(instance.getKubernetesNamespace());
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete Kubernetes resources
        try {
            kubernetesClient.pods().inNamespace(effectiveNamespace).withName(instance.getKubernetesPodName()).delete();
            
            // Delete the SSH service
            kubernetesClient.services().inNamespace(effectiveNamespace).withName(instance.getKubernetesPodName() + "-ssh").delete();
            
            log.info("Deleted Kubernetes pod and service for container {}", instance.getName());
        } catch (Exception e) {
            log.warn("Could not delete Kubernetes resources (development mode): {}", e.getMessage());
        }
        
        // Delete from database
        containerInstanceRepository.delete(instance);
        
        log.info("Deleted container {} by user {}", instance.getName(), user.getUsername());
    }
    
    /**
     * Get container logs
     */
    public String getContainerLogs(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        String effectiveNamespace = resolveNamespace(instance.getKubernetesNamespace());
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        try {
            return kubernetesClient.pods()
                    .inNamespace(effectiveNamespace)
                    .withName(instance.getKubernetesPodName())
                    .getLog();
        } catch (Exception e) {
            log.error("Failed to get logs for container {}: {}", instance.getName(), e.getMessage());
            return "Failed to retrieve logs: " + e.getMessage();
        }
    }
    
    /**
     * Create PVC for persistent storage
     */
    private void createPersistentVolumeClaim(String name, String size, String targetNamespace) {
        String pvcName = name + "-pvc";

        String effectiveNamespace = resolveNamespace(targetNamespace);
        if (kubernetesClient.persistentVolumeClaims().inNamespace(effectiveNamespace).withName(pvcName).get() != null) {
            log.debug("PVC {} already exists, reusing", pvcName);
            return;
        }

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName(pvcName)
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withAccessModes("ReadWriteOnce")
                    .withNewResources()
                        .addToRequests("storage", new Quantity(size != null ? size : "1Gi"))
                    .endResources()
                .endSpec()
                .build();

        kubernetesClient.persistentVolumeClaims().inNamespace(effectiveNamespace).resource(pvc).create();
    }
    
    /**
     * Update container status by checking Kubernetes pod status
     */
    public void updateContainerStatus(ContainerInstance instance) {
        String effectiveNamespace = resolveNamespace(instance.getKubernetesNamespace());
        try {
            log.debug("Checking pod status for: {}", instance.getKubernetesPodName());
            
            Pod pod = kubernetesClient.pods()
                    .inNamespace(effectiveNamespace)
                    .withName(instance.getKubernetesPodName())
                    .get();
            
            if (pod != null && pod.getStatus() != null) {
                String phase = pod.getStatus().getPhase();
                String currentStatus = instance.getStatus();
                
                // Only update if status has changed
                if (!phase.equals(currentStatus)) {
                    instance.setStatus(phase);
                    containerInstanceRepository.save(instance);
                    log.info("Updated container {} status from {} to {}", instance.getName(), currentStatus, phase);
                } else {
                    log.debug("Container {} status unchanged: {}", instance.getName(), phase);
                }
            } else {
                log.warn("Pod {} not found or has no status", instance.getKubernetesPodName());
                
                // If pod doesn't exist, mark as stopped
                if (!"Stopped".equals(instance.getStatus()) && !"Deleted".equals(instance.getStatus())) {
                    instance.setStatus("Stopped");
                    containerInstanceRepository.save(instance);
                    log.info("Marked container {} as Stopped (pod not found)", instance.getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get pod status from Kubernetes for {}: {}", instance.getKubernetesPodName(), e.getMessage());
            
            // Don't simulate in production - let the actual error be known
            // Only fallback to simulation if specifically in development mode
            if ("Creating".equals(instance.getStatus())) {
                log.warn("Container {} stuck in Creating state, will retry status check later", instance.getName());
            }
        }
    }
    
    /**
     * Check if user can access the container
     */
    private boolean canAccessContainer(ContainerInstance instance, User user) {
        // Students can only access their own containers
        if ("ROLE_STUDENT".equals(user.getRole())) {
            return instance.getOwner().getId().equals(user.getId());
        }
        
        // Teachers can access all containers
        return "ROLE_TEACHER".equals(user.getRole());
    }
    /**
     * Create NodePort service for SSH access to a container
     */
    private void createNodePortService(String containerName, Map<String, String> labels, String targetNamespace, IntOrString targetPort) {
        String effectiveNamespace = resolveNamespace(targetNamespace);
        try {
            // Calculate a unique NodePort (30000-32767 range in Kubernetes)
            int nodePort = 30000 + Math.abs(containerName.hashCode() % 2767);
            
            io.fabric8.kubernetes.api.model.Service service = new ServiceBuilder()
                    .withNewMetadata()
                        .withName(containerName + "-ssh")
                        .withNamespace(namespace)
                        .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                        .withType("NodePort")
                        .withSelector(labels)
                        .addNewPort()
                            .withName("ssh")
                            .withPort(22)
                            .withTargetPort(targetPort)
                            .withNodePort(nodePort)
                            .withProtocol("TCP")
                        .endPort()
                    .endSpec()
                    .build();
            
            kubernetesClient.services().inNamespace(effectiveNamespace).resource(service).create();
            
            log.info("Created NodePort service {}-ssh with port {} for SSH access", containerName, nodePort);
        } catch (Exception e) {
            log.warn("Could not create NodePort service for SSH access (development mode): {}", e.getMessage());
        }
    }

    /**
     * Get the NodePort assigned to a container's SSH service
     */
    public Integer getContainerSshPort(ContainerInstance instance) {
        String containerName = instance.getKubernetesPodName();
        String effectiveNamespace = resolveNamespace(instance.getKubernetesNamespace());
        try {
            io.fabric8.kubernetes.api.model.Service service = kubernetesClient.services()
                    .inNamespace(effectiveNamespace)
                    .withName(containerName + "-ssh")
                    .get();
            
            if (service != null && service.getSpec().getPorts() != null && !service.getSpec().getPorts().isEmpty()) {
                return service.getSpec().getPorts().get(0).getNodePort();
            }
        } catch (Exception e) {
            log.warn("Could not get NodePort for container {} (development mode): {}", containerName, e.getMessage());
        }
        
        // Fallback: calculate the same port we would have assigned
        return 30000 + Math.abs(containerName.hashCode() % 2767);
    }

    /**
     * Get Minikube IP address for SSH connections
     */
    public String getMinikubeIp() {
        try {
            // Try to get the actual Minikube IP
            Process process = Runtime.getRuntime().exec("minikube ip");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String ip = reader.readLine();
            if (ip != null && !ip.trim().isEmpty()) {
                log.info("Using Minikube IP: {}", ip);
                return ip.trim();
            }
        } catch (Exception e) {
            log.warn("Could not get Minikube IP: {}", e.getMessage());
        }
        
        // Fallback to localhost for development
        log.info("Using localhost as fallback IP");
        return "localhost";
    }

    /**
     * Check if a student can access a specific container by username
     */
    public boolean canStudentAccessContainer(Long containerId, String username) {
        try {
            ContainerInstance container = containerInstanceRepository.findById(containerId)
                    .orElse(null);
            
            if (container == null) {
                return false;
            }
            
            return container.getOwner().getUsername().equals(username);
        } catch (Exception e) {
            log.error("Error checking container access for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Public helper to reuse access checks across controllers
     */
    public boolean userCanAccessContainer(ContainerInstance instance, User user) {
        return canAccessContainer(instance, user);
    }

    /**
     * Find a container instance by ID
     */
    public ContainerInstance findById(Long id) {
        return containerInstanceRepository.findById(id).orElse(null);
    }

    private String resolveNamespace(String requestedNamespace) {
        return (requestedNamespace != null && !requestedNamespace.isBlank()) ? requestedNamespace : this.namespace;
    }
}
