package com.thesis.backend.model;

import com.thesis.backend.entity.User;
import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "container_templates")
public class ContainerTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String description;
    
    private String image;
    
    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    
    @ElementCollection
    @CollectionTable(name = "template_env_vars", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    private Map<String, String> environmentVariables = new HashMap<>();
    
    // Resource requirements
    private String cpuLimit;
    private String memoryLimit;
    private String cpuRequest;
    private String memoryRequest;
    
    // Command and arguments to run
    @Column(length = 1000)
    private String command;
    
    @ElementCollection
    @CollectionTable(name = "template_args", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "argument")
    private java.util.List<String> args = new java.util.ArrayList<>();

    // Whether this is a shared template that all teachers can use
    private boolean shared = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public String getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(String cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public String getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public String getCpuRequest() {
        return cpuRequest;
    }

    public void setCpuRequest(String cpuRequest) {
        this.cpuRequest = cpuRequest;
    }

    public String getMemoryRequest() {
        return memoryRequest;
    }

    public void setMemoryRequest(String memoryRequest) {
        this.memoryRequest = memoryRequest;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public java.util.List<String> getArgs() {
        return args;
    }

    public void setArgs(java.util.List<String> args) {
        this.args = args;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
    
    // Helper methods to convert to Kubernetes resource maps
    public Map<String, String> getResourceLimits() {
        Map<String, String> limits = new HashMap<>();
        if (cpuLimit != null && !cpuLimit.isEmpty()) {
            limits.put("cpu-limit", cpuLimit);
        }
        if (memoryLimit != null && !memoryLimit.isEmpty()) {
            limits.put("memory-limit", memoryLimit);
        }
        if (cpuRequest != null && !cpuRequest.isEmpty()) {
            limits.put("cpu-request", cpuRequest);
        }
        if (memoryRequest != null && !memoryRequest.isEmpty()) {
            limits.put("memory-request", memoryRequest);
        }
        return limits;
    }
}
