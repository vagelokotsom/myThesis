package com.thesis.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "container_templates")
public class ContainerTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "docker_image", nullable = false)
    private String dockerImage;
    
    @Column(name = "default_command")
    private String defaultCommand;
    
    @Column(name = "exposed_ports")
    private String exposedPorts; // JSON string of ports
    
    @Column(name = "environment_vars", columnDefinition = "TEXT")
    private String environmentVars; // JSON string of env vars
    
    @Column(name = "resource_limits", columnDefinition = "TEXT") 
    private String resourceLimits; // JSON string of resource limits
    
    @Column(name = "ssh_enabled")
    @Builder.Default
    private Boolean sshEnabled = true;
    
    @Column(name = "persistent_storage")
    @Builder.Default
    private Boolean persistentStorage = false;
    
    @Column(name = "storage_size")
    private String storageSize; // e.g., "1Gi"
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false; // Can be used by other teachers
    
    @Column(name = "category")
    private String category; // e.g., "Programming", "Data Science", "Web Development"
    
    @Column(name = "difficulty_level")
    private String difficultyLevel; // "Beginner", "Intermediate", "Advanced"
    
    @Column(name = "pre_installed_tools", columnDefinition = "TEXT")
    private String preInstalledTools; // JSON array of tools/packages
}
