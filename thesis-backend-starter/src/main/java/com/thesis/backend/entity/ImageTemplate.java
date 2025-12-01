
package com.thesis.backend.entity;


import jakarta.persistence.*;
import lombok.*;
import java.util.Map;
import java.util.HashMap;

@Entity
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ImageTemplate {
    @Id @GeneratedValue private Long id;
    private String name;
    private String dockerImage;
    private String description;

    /** Resource limits and requests: keys like 'cpu-limit', 'memory-limit', 'cpu-request', 'memory-request' */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "image_template_resource_limits", joinColumns = @JoinColumn(name = "image_template_id"))
    @MapKeyColumn(name = "resource_key")
    @Column(name = "resource_value")
    @Builder.Default
    private Map<String, String> resourceLimits = new HashMap<>();

    /** Environment variables for the container */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "image_template_env_vars", joinColumns = @JoinColumn(name = "image_template_id"))
    @MapKeyColumn(name = "env_key")
    @Column(name = "env_value")
    @Builder.Default
    private Map<String, String> environmentVars = new HashMap<>();

    /** Persistent storage required for the container */
    @Builder.Default
    private Boolean persistentStorage = false;

    /** Storage size for persistent volume (e.g., '1Gi') */
    @Builder.Default
    private String storageSize = "1Gi";
}
