package com.thesis.backend.controller;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.service.ContainerInstanceService;
import com.thesis.backend.service.KubernetesService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final KubernetesService kubeService;
    private final ContainerInstanceRepository containerRepo;
    private final ImageTemplateRepository imageRepo;
    private final ContainerInstanceService containerInstanceService;

    @Data
    public static class CreateContainerRequest {
        private Long imageId;  // Changed from templateId to imageId
        private Long studentId;
    }

    @PostMapping("/create/{imageId}")
    public ResponseEntity<?> create(@PathVariable Long imageId, @RequestParam String username) {
        ImageTemplate template = imageRepo.findById(imageId).orElseThrow();
        String podName = kubeService.createContainer(template.getDockerImage(), username);
        ContainerInstance instance = ContainerInstance.builder()
                .kubernetesPodName(podName)
                .status("Running")
                .name(podName)
                .build();
        return ResponseEntity.ok(containerRepo.save(instance));
    }

    /**
     * Create a container for a student (teacher only)
     */
    @PostMapping("/create-for-student")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> createContainerForStudent(
            @RequestBody CreateContainerRequest request,
            @AuthenticationPrincipal User teacher) {
        try {
            log.info("Teacher {} creating container for student {} using image {}", 
                    teacher.getUsername(), request.getStudentId(), request.getImageId());
            
            ContainerInstance instance = containerInstanceService.createContainerForStudent(
                    request.getImageId(), request.getStudentId(), teacher);
            
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            log.error("Failed to create container for student", e);
            return ResponseEntity.badRequest().body("Failed to create container: " + e.getMessage());
        }
    }

    /**
     * Get all containers (for teachers and admins)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<ContainerInstance>> getAllContainers() {
        try {
            List<ContainerInstance> containers = containerInstanceService.getAllContainers();
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            log.error("Failed to fetch all containers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get containers for the authenticated user (students see only their own)
     */
    @GetMapping("/my-containers")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<List<ContainerInstance>> getMyContainers(@AuthenticationPrincipal User user) {
        try {
            List<ContainerInstance> containers;
            if ("ROLE_TEACHER".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole())) {
                // Teachers can see all containers
                containers = containerInstanceService.getAllContainers();
            } else {
                // Students see only their own containers
                containers = containerInstanceService.getStudentContainers(user);
            }
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            log.error("Failed to fetch user containers for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get container statistics for dashboard
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getContainerStats() {
        try {
            List<ContainerInstance> containers = containerInstanceService.getAllContainers();
            
            long totalContainers = containers.size();
            long runningContainers = containers.stream()
                .filter(c -> "Running".equalsIgnoreCase(c.getStatus()))
                .count();
            long stoppedContainers = containers.stream()
                .filter(c -> "Stopped".equalsIgnoreCase(c.getStatus()))
                .count();
            long pendingContainers = containers.stream()
                .filter(c -> "Pending".equalsIgnoreCase(c.getStatus()))
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalContainers);
            stats.put("running", runningContainers);
            stats.put("stopped", stoppedContainers);
            stats.put("pending", pendingContainers);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch container statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Refresh status of a specific container
     */
    @PostMapping("/{id}/refresh-status")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<ContainerInstance> refreshContainerStatus(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            ContainerInstance container = containerInstanceService.findById(id);
            if (container == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if user can access this container
            if ("ROLE_STUDENT".equals(user.getRole()) && !container.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            // Refresh the status from Kubernetes
            containerInstanceService.updateContainerStatus(container);
            
            // Return the updated container
            ContainerInstance updated = containerInstanceService.findById(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to refresh container status for container ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Refresh all container statuses
     */
    @PostMapping("/refresh-all-statuses")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshAllContainerStatuses() {
        try {
            List<ContainerInstance> containers = containerInstanceService.getAllContainers();
            int updated = 0;
            
            for (ContainerInstance container : containers) {
                try {
                    containerInstanceService.updateContainerStatus(container);
                    updated++;
                } catch (Exception e) {
                    log.warn("Failed to update status for container {}: {}", container.getName(), e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalContainers", containers.size());
            result.put("updatedContainers", updated);
            result.put("message", "Status refresh completed");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to refresh all container statuses", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}/ssh-info")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or @containerInstanceService.canStudentAccessContainer(#id, authentication.name)")
    public ResponseEntity<?> getSshInfo(@PathVariable Long id) {
        try {
            ContainerInstance container = containerRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container not found"));
            
            // Generate SSH connection details
            Map<String, Object> sshInfo = new HashMap<>();
            sshInfo.put("containerId", container.getId());
            sshInfo.put("containerName", container.getName());
            sshInfo.put("status", container.getStatus());
            sshInfo.put("podName", container.getKubernetesPodName());
            
            if ("Running".equals(container.getStatus())) {
                // Get real SSH connection details from Kubernetes
                String minikubeIp = containerInstanceService.getMinikubeIp();
                Integer sshPort = containerInstanceService.getContainerSshPort(container.getKubernetesPodName());
                
                sshInfo.put("host", minikubeIp);
                sshInfo.put("port", sshPort);
                sshInfo.put("username", "root");
                sshInfo.put("password", "student123");
                sshInfo.put("dockerImage", "thesis-ssh-container:latest");
                sshInfo.put("ready", true);
                
                // Provide both direct and port-forward instructions
                sshInfo.put("instructions", "Connect using: ssh -p " + sshPort + " root@" + minikubeIp);
                sshInfo.put("note", "This connects to a real SSH-enabled container running in Minikube");
                
                // Add port-forward instructions for better compatibility
                String serviceName = container.getKubernetesPodName() + "-ssh";
                int localPort = 8023; // You can change this to any available port
                sshInfo.put("portForwardCommand", "kubectl port-forward service/" + serviceName + " " + localPort + ":22");
                sshInfo.put("portForwardSsh", "ssh -p " + localPort + " root@127.0.0.1");
                sshInfo.put("alternativeNote", "If direct connection fails (common on macOS), use port forwarding method below");
                
                // Add port explanations
                sshInfo.put("portExplanation", Map.of(
                    "nodePort", "Port " + sshPort + " is assigned by Kubernetes (NodePort) - this changes with each container",
                    "localPort", "Port " + localPort + " is a local port we choose for convenience - you can use any free port",
                    "why", "Different ports serve different purposes: NodePort for direct access, local port for tunneling"
                ));
                
                // Add step-by-step instructions for better user experience
                sshInfo.put("stepByStepInstructions", Map.of(
                    "step1", "Open a terminal/command prompt",
                    "step2", "Run the port-forward command: kubectl port-forward service/" + serviceName + " " + localPort + ":22",
                    "step3", "Open a new terminal window (keep the first one running)",
                    "step4", "Connect via SSH: ssh -p " + localPort + " root@127.0.0.1",
                    "step5", "Enter password when prompted: student123"
                ));
                
                // Add troubleshooting tips
                sshInfo.put("troubleshooting", Map.of(
                    "connectionRefused", "Make sure the port-forward command is running in a separate terminal",
                    "passwordFailed", "Use password: student123 (case sensitive)",
                    "commandNotFound", "Make sure kubectl is installed and configured for your cluster",
                    "portInUse", "Try a different port like 8024:22 instead of 8023:22"
                ));
            } else {
                sshInfo.put("ready", false);
                sshInfo.put("message", "Container is not running yet. Please wait for it to start.");
            }
            
            return ResponseEntity.ok(sshInfo);
        } catch (Exception e) {
            log.error("Failed to get SSH info", e);
            return ResponseEntity.badRequest().body("Failed to get SSH info: " + e.getMessage());
        }
    }
}
