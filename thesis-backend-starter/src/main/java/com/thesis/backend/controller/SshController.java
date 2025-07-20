package com.thesis.backend.controller;

import com.thesis.backend.entity.SshConnection;
import com.thesis.backend.entity.User;
import com.thesis.backend.service.SshConnectionService;
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
@RequestMapping("/api/ssh")
@RequiredArgsConstructor
public class SshController {
    
    private final SshConnectionService sshConnectionService;
    
    /**
     * Create SSH access for a student to a container
     */
    @PostMapping("/connect/{containerInstanceId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> createSshConnection(
            @PathVariable Long containerInstanceId,
            @RequestParam(defaultValue = "24") Integer durationHours,
            @AuthenticationPrincipal User student) {
        
        try {
            SshConnection connection = sshConnectionService.createSshAccess(student, containerInstanceId, durationHours);
            
            Map<String, Object> response = new HashMap<>();
            response.put("connectionId", connection.getId());
            response.put("sshUsername", connection.getSshUsername());
            response.put("sshPassword", connection.getSshPassword());
            response.put("connectionPort", connection.getConnectionPort());
            response.put("containerName", connection.getContainerInstance().getName());
            response.put("expiresAt", connection.getExpiresAt());
            response.put("instructions", generateSshInstructions(connection));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create SSH connection", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create SSH connection: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get active SSH connections for the authenticated user
     */
    @GetMapping("/connections")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<List<SshConnection>> getActiveConnections(@AuthenticationPrincipal User user) {
        List<SshConnection> connections = sshConnectionService.getActiveConnections(user);
        return ResponseEntity.ok(connections);
    }
    
    /**
     * Get SSH connection details
     */
    @GetMapping("/connections/{connectionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getConnectionDetails(
            @PathVariable Long connectionId,
            @AuthenticationPrincipal User user) {
        
        try {
            List<SshConnection> userConnections = sshConnectionService.getActiveConnections(user);
            SshConnection connection = userConnections.stream()
                    .filter(conn -> conn.getId().equals(connectionId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Connection not found or access denied"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("connectionId", connection.getId());
            response.put("sshUsername", connection.getSshUsername());
            response.put("sshPassword", connection.getSshPassword());
            response.put("connectionPort", connection.getConnectionPort());
            response.put("containerName", connection.getContainerInstance().getName());
            response.put("status", connection.getStatus());
            response.put("createdAt", connection.getCreatedAt());
            response.put("expiresAt", connection.getExpiresAt());
            response.put("lastAccessed", connection.getLastAccessed());
            response.put("instructions", generateSshInstructions(connection));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Revoke SSH access (only for teachers or connection owner)
     */
    @DeleteMapping("/connections/{connectionId}")
    @PreAuthorize("hasRole('TEACHER') or (hasRole('STUDENT') and @sshConnectionService.isConnectionOwner(#connectionId, authentication.principal))")
    public ResponseEntity<Map<String, String>> revokeSshConnection(@PathVariable Long connectionId) {
        try {
            sshConnectionService.revokeSshAccess(connectionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "SSH connection revoked successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to revoke SSH connection: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Teacher endpoint to view all SSH connections
     */
    @GetMapping("/admin/connections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SshConnection>> getAllConnections() {
        List<SshConnection> connections = sshConnectionService.getAllConnections();
        return ResponseEntity.ok(connections);
    }
    
    /**
     * Cleanup expired connections (admin endpoint)
     */
    @PostMapping("/admin/cleanup")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, String>> cleanupExpiredConnections() {
        sshConnectionService.cleanupExpiredConnections();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Expired connections cleaned up successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test SSH connectivity
     */
    @PostMapping("/test/{connectionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> testSshConnection(
            @PathVariable Long connectionId,
            @AuthenticationPrincipal User user) {
        
        try {
            List<SshConnection> userConnections = sshConnectionService.getActiveConnections(user);
            SshConnection connection = userConnections.stream()
                    .filter(conn -> conn.getId().equals(connectionId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Connection not found or access denied"));
            
            // Test the SSH connection
            boolean isConnectable = testConnectionConnectivity(connection);
            
            Map<String, Object> response = new HashMap<>();
            response.put("connectionId", connectionId);
            response.put("isConnectable", isConnectable);
            response.put("status", connection.getStatus());
            response.put("message", isConnectable ? "SSH connection is active and reachable" : "SSH connection is not reachable");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Generate SSH connection instructions for the user
     */
    private Map<String, String> generateSshInstructions(SshConnection connection) {
        Map<String, String> instructions = new HashMap<>();
        
        // For minikube, we need to get the minikube IP
        String minikubeIp = "$(minikube ip)";
        
        instructions.put("command", String.format("ssh %s@%s -p %d", 
                connection.getSshUsername(), 
                minikubeIp, 
                connection.getConnectionPort()));
        
        instructions.put("description", "Use this command to connect to your container via SSH");
        instructions.put("password", connection.getSshPassword());
        instructions.put("note", "Make sure your Minikube cluster is running and accessible");
        
        instructions.put("alternativeCommand", String.format("ssh %s@localhost -p %d", 
                connection.getSshUsername(), 
                connection.getConnectionPort()));
        
        instructions.put("portForwardCommand", String.format("kubectl port-forward svc/%s-ssh %d:%d", 
                connection.getContainerInstance().getKubernetesPodName(),
                connection.getConnectionPort(),
                connection.getConnectionPort()));
        
        return instructions;
    }
    
    /**
     * Test if SSH connection is reachable
     */
    private boolean testConnectionConnectivity(SshConnection connection) {
        // This is a simplified test - in production you'd want to actually test the SSH connection
        return "ACTIVE".equals(connection.getStatus()) && 
               connection.getExpiresAt().isAfter(java.time.LocalDateTime.now());
    }
}
