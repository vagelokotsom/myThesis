package com.thesis.backend.service;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.SshConnection;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.SshConnectionRepository;
import com.thesis.backend.repository.ContainerInstanceRepository;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SshConnectionService {
    
    private final SshConnectionRepository sshConnectionRepository;
    private final ContainerInstanceRepository containerInstanceRepository;
    private final KubernetesClient kubernetesClient;
    
    @Value("${ssh.container.base-port:30000}")
    private int baseSshPort;
    
    @Value("${ssh.container.namespace:default}")
    private String namespace;
    
    private final SecureRandom random = new SecureRandom();
    
    /**
     * Create SSH access for a student to a specific container
     */
    public SshConnection createSshAccess(User student, Long containerInstanceId, Integer durationHours) {
        ContainerInstance container = containerInstanceRepository.findById(containerInstanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        
        // Check if active connection already exists
        Optional<SshConnection> existingConnection = sshConnectionRepository
                .findByUserAndContainerInstanceAndStatus(student, container, "ACTIVE");
        
        if (existingConnection.isPresent()) {
            log.info("Returning existing SSH connection for user {} to container {}", 
                    student.getUsername(), container.getName());
            return existingConnection.get();
        }
        
        // Create SSH-enabled container if not already done
        String sshEnabledPodName = ensureSshEnabledContainer(container);
        
        // Generate SSH credentials
        String sshUsername = generateSshUsername(student.getUsername());
        String sshPassword = generateSecurePassword();
        int sshPort = allocatePort();
        
        // Create SSH connection record
        SshConnection sshConnection = SshConnection.builder()
                .user(student)
                .containerInstance(container)
                .sshUsername(sshUsername)
                .sshPassword(sshPassword)
                .connectionPort(sshPort)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusHours(durationHours != null ? durationHours : 24))
                .build();
        
        // Update container with SSH details
        updateContainerWithSshAccess(container, sshUsername, sshPassword, sshPort);
        
        // Expose SSH port via Kubernetes Service
        createSshService(container, sshPort);
        
        return sshConnectionRepository.save(sshConnection);
    }
    
    /**
     * Get active SSH connections for a user
     */
    public List<SshConnection> getActiveConnections(User user) {
        return sshConnectionRepository.findActiveConnectionsByUser(user);
    }
    
    /**
     * Revoke SSH access
     */
    public void revokeSshAccess(Long connectionId) {
        SshConnection connection = sshConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("SSH connection not found"));
        
        connection.setStatus("INACTIVE");
        sshConnectionRepository.save(connection);
        
        // Remove SSH service
        removeSshService(connection.getContainerInstance());
        
        log.info("SSH access revoked for connection {}", connectionId);
    }
    
    /**
     * Clean up expired connections
     */
    public void cleanupExpiredConnections() {
        List<SshConnection> expiredConnections = sshConnectionRepository
                .findExpiredConnections(LocalDateTime.now());
        
        for (SshConnection connection : expiredConnections) {
            connection.setStatus("EXPIRED");
            sshConnectionRepository.save(connection);
            removeSshService(connection.getContainerInstance());
        }
        
        log.info("Cleaned up {} expired SSH connections", expiredConnections.size());
    }
    
    /**
     * Authenticate SSH connection
     */
    public boolean authenticateSshUser(String username, String password) {
        Optional<SshConnection> connection = sshConnectionRepository.findActiveBySshUsername(username);
        
        if (connection.isPresent()) {
            SshConnection conn = connection.get();
            if (conn.getSshPassword().equals(password) && 
                conn.getExpiresAt().isAfter(LocalDateTime.now())) {
                
                // Update last accessed time
                conn.setLastAccessed(LocalDateTime.now());
                sshConnectionRepository.save(conn);
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get container details for SSH session
     */
    public Optional<SshConnection> getConnectionByUsername(String sshUsername) {
        return sshConnectionRepository.findActiveBySshUsername(sshUsername);
    }
    
    /**
     * Ensure container has SSH capability
     */
    private String ensureSshEnabledContainer(ContainerInstance container) {
        String podName = container.getKubernetesPodName();
        Pod existingPod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
        
        if (existingPod == null) {
            throw new RuntimeException("Container pod not found: " + podName);
        }
        
        // Check if pod already has SSH capability
        if (podHasSshCapability(existingPod)) {
            return podName;
        }
        
        // Create new SSH-enabled pod
        String sshPodName = podName + "-ssh";
        createSshEnabledPod(existingPod, sshPodName);
        
        // Update container instance
        container.setKubernetesPodName(sshPodName);
        containerInstanceRepository.save(container);
        
        return sshPodName;
    }
    
    /**
     * Check if pod has SSH capability
     */
    private boolean podHasSshCapability(Pod pod) {
        return pod.getSpec().getContainers().stream()
                .anyMatch(container -> container.getPorts() != null && 
                         container.getPorts().stream()
                                 .anyMatch(port -> port.getContainerPort() == 22));
    }
    
    /**
     * Create SSH-enabled pod
     */
    private void createSshEnabledPod(Pod originalPod, String newPodName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", newPodName);
        labels.put("type", "ssh-container");
        
        Pod sshPod = new PodBuilder()
                .withNewMetadata()
                    .withName(newPodName)
                    .withNamespace(namespace)
                    .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName("ssh-container")
                        .withImage("thesis-ssh-container:latest")  // Use our custom SSH image
                        .addNewEnv()
                            .withName("ROOT_PASSWORD")
                            .withValue("rootpass123")
                        .endEnv()
                        .addNewPort()
                            .withContainerPort(22)
                            .withProtocol("TCP")
                        .endPort()
                        .withNewResources()
                            .addToRequests("memory", new io.fabric8.kubernetes.api.model.Quantity("256Mi"))
                            .addToRequests("cpu", new io.fabric8.kubernetes.api.model.Quantity("100m"))
                            .addToLimits("memory", new io.fabric8.kubernetes.api.model.Quantity("512Mi"))
                            .addToLimits("cpu", new io.fabric8.kubernetes.api.model.Quantity("500m"))
                        .endResources()
                    .endContainer()
                .endSpec()
                .build();
        
        kubernetesClient.pods().inNamespace(namespace).create(sshPod);
        log.info("Created SSH-enabled pod: {}", newPodName);
    }
    
    /**
     * Update container with SSH access
     */
    private void updateContainerWithSshAccess(ContainerInstance container, String username, String password, int port) {
        String podName = container.getKubernetesPodName();
        
        // Update the pod with SSH user environment variable
        try {
            // Get the current pod
            Pod currentPod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
            if (currentPod == null) {
                throw new RuntimeException("Pod not found: " + podName);
            }
            
            // Create environment variable for SSH users
            String sshUsers = username + ":" + password;
            
            // Update the pod with new environment variable
            Pod updatedPod = new PodBuilder(currentPod)
                    .editSpec()
                        .editContainer(0)
                            .addNewEnv()
                                .withName("SSH_USERS")
                                .withValue(sshUsers)
                            .endEnv()
                        .endContainer()
                    .endSpec()
                    .build();
            
            // Apply the update
            kubernetesClient.pods().inNamespace(namespace).withName(podName).patch(updatedPod);
            
            // Wait a moment for the pod to update
            Thread.sleep(2000);
            
            // Execute command to create user in the running container
            String setupCommand = String.format(
                "useradd -m -s /bin/bash %s && echo '%s:%s' | chpasswd && usermod -aG sudo %s && mkdir -p /home/%s/workspace && chown %s:%s /home/%s/workspace",
                username, username, password, username, username, username, username, username
            );
            
            kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .redirectingInput()
                    .redirectingOutput()
                    .exec("bash", "-c", setupCommand);
            
            log.info("SSH user {} created in container {}", username, podName);
        } catch (Exception e) {
            log.error("Failed to create SSH user in container: {}", e.getMessage());
            // Don't throw exception - the container might still work
            log.warn("Continuing without dynamic user creation. User creation will happen on container restart.");
        }
    }
    
    /**
     * Create Kubernetes service to expose SSH port
     */
    private void createSshService(ContainerInstance container, int port) {
        String serviceName = container.getKubernetesPodName() + "-ssh";
        
        Map<String, String> selector = new HashMap<>();
        selector.put("app", container.getKubernetesPodName());
        
        Service sshService = new ServiceBuilder()
                .withNewMetadata()
                    .withName(serviceName)
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withSelector(selector)
                    .addNewPort()
                        .withPort(port)
                        .withTargetPort(new IntOrString(22))
                        .withNodePort(port)
                        .withProtocol("TCP")
                    .endPort()
                    .withType("NodePort")
                .endSpec()
                .build();
        
        kubernetesClient.services().inNamespace(namespace).create(sshService);
        log.info("Created SSH service {} on port {}", serviceName, port);
    }
    
    /**
     * Remove SSH service
     */
    private void removeSshService(ContainerInstance container) {
        String serviceName = container.getKubernetesPodName() + "-ssh";
        kubernetesClient.services().inNamespace(namespace).withName(serviceName).delete();
        log.info("Removed SSH service: {}", serviceName);
    }
    
    /**
     * Generate unique SSH username
     */
    private String generateSshUsername(String baseUsername) {
        return "student_" + baseUsername + "_" + System.currentTimeMillis();
    }
    
    /**
     * Generate secure password
     */
    private String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
    
    /**
     * Allocate available port for SSH
     */
    private int allocatePort() {
        // Simple port allocation - in production, you'd want more sophisticated logic
        return baseSshPort + random.nextInt(5000);
    }
    
    /**
     * Check if user owns the SSH connection
     */
    public boolean isConnectionOwner(Long connectionId, User user) {
        return sshConnectionRepository.findById(connectionId)
                .map(connection -> connection.getUser().getId().equals(user.getId()))
                .orElse(false);
    }
    
    /**
     * Get all SSH connections for admin view
     */
    public List<SshConnection> getAllConnections() {
        return sshConnectionRepository.findAll();
    }
}
