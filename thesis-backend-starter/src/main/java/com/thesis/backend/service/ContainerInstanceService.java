package com.thesis.backend.service;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ContainerTemplate;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ContainerTemplateRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.repository.UserRepository;
import io.fabric8.kubernetes.api.model.Container;
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
    
    @Value("${ssh.container.namespace:default}")
    private String namespace;
    
    /**
     * Create a container instance from a template for a student
     */
    public ContainerInstance createContainerFromTemplate(Long templateId, User student, User teacher) {
        ContainerTemplate template = containerTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Container template not found"));
        
        // Generate unique name for the container
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String containerName = String.format("%s-%s-%s", 
                template.getName().toLowerCase().replaceAll("[^a-z0-9]", "-"),
                student.getUsername().toLowerCase(),
                timestamp);
        
        // Create Kubernetes pod from template
        String podName = createKubernetesPod(template, containerName, student);
        
        // Create container instance record
        ContainerInstance instance = ContainerInstance.builder()
                .name(containerName)
                .status("Creating")
                .kubernetesPodName(podName)
                .owner(student)
                .imageTemplate(null) // We'll set this based on template if needed
                .build();
        
        ContainerInstance savedInstance = containerInstanceRepository.save(instance);
        
        // Update status after pod is created
        updateContainerStatus(savedInstance);
        
        log.info("Created container instance {} for student {} from template {}", 
                containerName, student.getUsername(), template.getName());
        
        return savedInstance;
    }
    
    /**
     * Create a container instance from an image template for a student (by IDs)
     */
    public ContainerInstance createContainerForStudent(Long imageId, Long studentId, User teacher) {
        // Find the student
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Verify the user is actually a student
        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new RuntimeException("User is not a student");
        }
        
        // For now, we'll use the existing simple container creation
        // TODO: Integrate with the more complex template system later
        return createSimpleContainerForStudent(imageId, student, teacher);
    }

    private ContainerInstance createSimpleContainerForStudent(Long imageId, User student, User teacher) {
        // Find the image template
        ImageTemplate imageTemplate = imageTemplateRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image template not found with id: " + imageId));
        
        // Generate unique name for the container
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String containerName = String.format("container-%s-%s", 
                student.getUsername().toLowerCase(),
                timestamp);
        
        // Create Kubernetes pod
        String podName = createSimpleKubernetesPod(imageTemplate, containerName, student);
        
        // Create container instance record
        ContainerInstance instance = ContainerInstance.builder()
                .name(containerName)
                .status("Creating")
                .kubernetesPodName(podName)
                .owner(student)
                .imageTemplate(imageTemplate) // Set the ImageTemplate
                .build();
        
        ContainerInstance savedInstance = containerInstanceRepository.save(instance);
        
        // Update status after pod creation
        updateContainerStatus(savedInstance);
        
        log.info("Created container instance {} for student {} using image template {} by teacher {}", 
                containerName, student.getUsername(), imageTemplate.getName(), teacher.getUsername());
        
        return savedInstance;
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
        
        // Check authorization
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete the Kubernetes pod and service
        try {
            kubernetesClient.pods().inNamespace(namespace).withName(instance.getKubernetesPodName()).delete();
            kubernetesClient.services().inNamespace(namespace).withName(instance.getKubernetesPodName() + "-ssh").delete();
            
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
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // For now, we'll recreate the pod since Kubernetes doesn't support start/stop
        // In a production environment, you might want to use deployments instead
        instance.setStatus("Starting");
        containerInstanceRepository.save(instance);
        
        log.info("Started container {} by user {}", instance.getName(), user.getUsername());
    }
    
    /**
     * Delete a container permanently
     */
    public void deleteContainer(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete Kubernetes resources
        try {
            kubernetesClient.pods().inNamespace(namespace).withName(instance.getKubernetesPodName()).delete();
            
            // Delete the SSH service
            kubernetesClient.services().inNamespace(namespace).withName(instance.getKubernetesPodName() + "-ssh").delete();
            
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
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        try {
            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(instance.getKubernetesPodName())
                    .getLog();
        } catch (Exception e) {
            log.error("Failed to get logs for container {}: {}", instance.getName(), e.getMessage());
            return "Failed to retrieve logs: " + e.getMessage();
        }
    }
    
    /**
     * Create Kubernetes pod from template
     */
    private String createKubernetesPod(ContainerTemplate template, String containerName, User student) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", containerName);
        labels.put("owner", student.getUsername());
        labels.put("type", "student-container");
        labels.put("ssh-enabled", template.getSshEnabled().toString());
        
        // Start building container
        ContainerBuilder containerBuilder = new ContainerBuilder()
                .withName("main-container");
        
        // Set image based on SSH requirements
        if (template.getSshEnabled()) {
            containerBuilder = containerBuilder
                    .withImage("thesis-ssh-container:latest")
                    .addNewPort()
                        .withContainerPort(22)
                        .withProtocol("TCP")
                    .endPort()
                    .addNewEnv()
                        .withName("ROOT_PASSWORD")
                        .withValue("rootpass123")
                    .endEnv();
        } else {
            containerBuilder = containerBuilder.withImage(template.getDockerImage());
        }
        
        // Add environment variables from template
        if (template.getEnvironmentVars() != null && !template.getEnvironmentVars().isEmpty()) {
            containerBuilder = containerBuilder
                    .addNewEnv()
                        .withName("WORKSPACE_USER")
                        .withValue(student.getUsername())
                    .endEnv();
        }
        
        // Add resource limits if specified
        if (template.getResourceLimits() != null && !template.getResourceLimits().isEmpty()) {
            containerBuilder = containerBuilder
                    .withNewResources()
                        .addToRequests("memory", new Quantity("256Mi"))
                        .addToRequests("cpu", new Quantity("100m"))
                        .addToLimits("memory", new Quantity("512Mi"))
                        .addToLimits("cpu", new Quantity("500m"))
                    .endResources();
        }
        
        // Add persistent volume mount if required
        if (template.getPersistentStorage()) {
            containerBuilder = containerBuilder
                    .addNewVolumeMount()
                        .withName("workspace-storage")
                        .withMountPath("/workspace")
                    .endVolumeMount();
        }
        
        // Build the pod spec
        PodSpecBuilder podSpecBuilder = new PodSpecBuilder()
                .addToContainers(containerBuilder.build());
        
        // Add persistent volume if required
        if (template.getPersistentStorage()) {
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
                    .withNamespace(namespace)
                    .withLabels(labels)
                .endMetadata()
                .withSpec(podSpecBuilder.build())
                .build();
        
        // Create PVC if persistent storage is required
        if (template.getPersistentStorage()) {
            createPersistentVolumeClaim(containerName, template.getStorageSize());
        }
        
        // Create the pod
        kubernetesClient.pods().inNamespace(namespace).resource(pod).create();        
        return containerName;
    }

    /**
     * Create PVC for persistent storage
     */
    private void createPersistentVolumeClaim(String name, String size) {
        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName(name + "-pvc")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withAccessModes("ReadWriteOnce")
                    .withNewResources()
                        .addToRequests("storage", new Quantity(size != null ? size : "1Gi"))
                    .endResources()
                .endSpec()
                .build();
        
        kubernetesClient.persistentVolumeClaims().inNamespace(namespace).resource(pvc).create();
    }
    
    /**
     * Update container status by checking Kubernetes pod status
     */
    public void updateContainerStatus(ContainerInstance instance) {
        try {
            log.debug("Checking pod status for: {}", instance.getKubernetesPodName());
            
            Pod pod = kubernetesClient.pods()
                    .inNamespace(namespace)
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
     * Create a simple Kubernetes pod for student containers with SSH access
     * Uses the SSH-enabled Docker image and creates a NodePort service for external access
     */
    private String createSimpleKubernetesPod(ImageTemplate imageTemplate, String containerName, User student) {
        try {
            Map<String, String> labels = new HashMap<>();
            labels.put("app", containerName);
            labels.put("owner", student.getUsername());
            labels.put("type", "student-container");
            labels.put("ssh-enabled", "true");
            
            // Create a container with SSH enabled using our custom SSH image
            Container container = new ContainerBuilder()
                    .withName("main-container")
                    .withImage("thesis-ssh-container:latest") // Use our SSH-enabled image
                    .withImagePullPolicy("Never") // Use local image in Minikube
                    .addNewPort()
                        .withContainerPort(22)
                        .withProtocol("TCP")
                        .withName("ssh")
                    .endPort()
                    .addNewEnv()
                        .withName("ROOT_PASSWORD")
                        .withValue("student123") // Simple password for educational purposes
                    .endEnv()
                    .addNewEnv()
                        .withName("SSH_ENABLED")
                        .withValue("true")
                    .endEnv()
                    .addNewEnv()
                        .withName("WORKSPACE_USER")
                        .withValue(student.getUsername())
                    .endEnv()
                    .withNewResources()
                        .addToRequests("memory", new Quantity("256Mi"))
                        .addToRequests("cpu", new Quantity("100m"))
                        .addToLimits("memory", new Quantity("512Mi"))
                        .addToLimits("cpu", new Quantity("500m"))
                    .endResources()
                    .build();
            
            // Build the pod
            Pod pod = new PodBuilder()
                    .withNewMetadata()
                        .withName(containerName)
                        .withNamespace(namespace)
                        .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                        .addToContainers(container)
                        .withRestartPolicy("Always")
                    .endSpec()
                    .build();
            
            // Create the pod
            kubernetesClient.pods().inNamespace(namespace).resource(pod).create();
            
            // Create NodePort service for SSH access
            createNodePortService(containerName, labels);
            
            log.info("Created Kubernetes pod {} with SSH-enabled image for student {}", 
                    containerName, student.getUsername());
                    
        } catch (Exception e) {
            // Log the actual error for debugging
            log.error("Failed to create Kubernetes pod {}: {}", containerName, e.getMessage(), e);
            log.info("Simulating pod creation for {} - student {} with SSH-enabled image", 
                    containerName, student.getUsername());
        }
        
        return containerName;
    }
    
    /**
     * Create NodePort service for SSH access to a container
     */
    private void createNodePortService(String containerName, Map<String, String> labels) {
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
                            .withTargetPort(new IntOrString(22))
                            .withNodePort(nodePort)
                            .withProtocol("TCP")
                        .endPort()
                    .endSpec()
                    .build();
            
            kubernetesClient.services().inNamespace(namespace).resource(service).create();
            
            log.info("Created NodePort service {}-ssh with port {} for SSH access", containerName, nodePort);
        } catch (Exception e) {
            log.warn("Could not create NodePort service for SSH access (development mode): {}", e.getMessage());
        }
    }

    /**
     * Get the NodePort assigned to a container's SSH service
     */
    public Integer getContainerSshPort(String containerName) {
        try {
            io.fabric8.kubernetes.api.model.Service service = kubernetesClient.services()
                    .inNamespace(namespace)
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
     * Find a container instance by ID
     */
    public ContainerInstance findById(Long id) {
        return containerInstanceRepository.findById(id).orElse(null);
    }
}
