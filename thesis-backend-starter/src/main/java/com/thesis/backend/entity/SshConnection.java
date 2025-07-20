package com.thesis.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ssh_connections")
public class SshConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "container_instance_id", nullable = false)
    private ContainerInstance containerInstance;
    
    @Column(name = "ssh_username", nullable = false)
    private String sshUsername;
    
    @Column(name = "ssh_password")
    private String sshPassword;
    
    @Column(name = "ssh_public_key", columnDefinition = "TEXT")
    private String sshPublicKey;
    
    @Column(name = "connection_port")
    private Integer connectionPort;
    
    @Column(name = "container_ip")
    private String containerIp;
    
    @Column(name = "status")
    private String status; // ACTIVE, INACTIVE, EXPIRED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            // Default expiration: 24 hours from creation
            expiresAt = createdAt.plusHours(24);
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
