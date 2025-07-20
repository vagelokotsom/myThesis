package com.thesis.backend.service;

import com.thesis.backend.model.KubernetesNamespace;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NamespaceService {

    @Autowired
    private KubernetesClient kubernetesClient;

    /**
     * Get all namespaces
     */
    public List<KubernetesNamespace> getAllNamespaces() {
        return kubernetesClient.namespaces().list().getItems()
                .stream().map(this::mapNamespaceToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get namespace by name
     */
    public KubernetesNamespace getNamespace(String name) {
        Namespace namespace = kubernetesClient.namespaces().withName(name).get();
        if (namespace == null) {
            return null;
        }
        return mapNamespaceToDto(namespace);
    }

    /**
     * Create a new namespace
     */
    public KubernetesNamespace createNamespace(String name, Map<String, String> labels) {
        Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withLabels(labels)
                .endMetadata()
                .build();

        Namespace createdNamespace = kubernetesClient.namespaces().create(namespace);
        return mapNamespaceToDto(createdNamespace);
    }

    /**
     * Delete a namespace
     */
    public boolean deleteNamespace(String name) {
        return !kubernetesClient.namespaces().withName(name).delete().isEmpty();
    }

    /**
     * Map Kubernetes Namespace to a simplified DTO
     */
    private KubernetesNamespace mapNamespaceToDto(Namespace namespace) {
        KubernetesNamespace kubernetesNamespace = new KubernetesNamespace();
        kubernetesNamespace.setName(namespace.getMetadata().getName());
        kubernetesNamespace.setStatus(
                namespace.getStatus() != null ? namespace.getStatus().getPhase() : null);
        
        if (namespace.getMetadata().getLabels() != null) {
            kubernetesNamespace.setLabels(namespace.getMetadata().getLabels());
        }
        
        if (namespace.getMetadata().getCreationTimestamp() != null) {
            kubernetesNamespace.setCreationTimestamp(
                    namespace.getMetadata().getCreationTimestamp());
        }
        
        return kubernetesNamespace;
    }
}
