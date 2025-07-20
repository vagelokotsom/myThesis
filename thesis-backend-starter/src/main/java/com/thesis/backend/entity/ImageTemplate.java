
package com.thesis.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ImageTemplate {
    @Id @GeneratedValue private Long id;
    private String name;
    private String dockerImage;
    private String description;
}
