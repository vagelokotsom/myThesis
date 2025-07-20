package com.thesis.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.thesis.backend.repository")
@EntityScan("com.thesis.backend.entity")
@ComponentScan("com.thesis.backend")
public class ThesisBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThesisBackendApplication.class, args);
    }
}
