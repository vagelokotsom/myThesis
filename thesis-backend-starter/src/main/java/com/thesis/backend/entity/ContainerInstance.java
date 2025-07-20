
package com.thesis.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContainerInstance {
    @Id @GeneratedValue private Long id;
    private String name;
    private String status;
    private String kubernetesPodName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    private User owner;

    @ManyToOne
    private ImageTemplate imageTemplate;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
