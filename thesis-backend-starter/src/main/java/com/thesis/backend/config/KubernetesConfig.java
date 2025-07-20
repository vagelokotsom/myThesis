package com.thesis.backend.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class KubernetesConfig {

    @Value("${kubernetes.namespace:default}")
    private String namespace;

    @Value("${kubernetes.config.auto:true}")
    private Boolean autoConfig;

    @Bean
    public KubernetesClient kubernetesClient() {
        try {
            Config config;
            
            if (autoConfig) {
                // Try to auto-detect configuration (works with minikube, in-cluster, etc.)
                config = Config.autoConfigure(null);
                log.info("Using auto-configured Kubernetes client");
            } else {
                // Fallback to manual configuration
                config = new ConfigBuilder()
                        .withNamespace(namespace)
                        .withTrustCerts(true)
                        .build();
                log.info("Using manual Kubernetes client configuration");
            }
            
            log.info("Kubernetes client configured - Master URL: {}, Namespace: {}", 
                    config.getMasterUrl(), config.getNamespace());
            
            return new DefaultKubernetesClient(config);
            
        } catch (Exception e) {
            log.error("Failed to configure Kubernetes client: {}", e.getMessage());
            log.warn("Creating Kubernetes client with default configuration for development");
            
            // Return a client with basic configuration for development
            return new DefaultKubernetesClient();
        }
    }
}
