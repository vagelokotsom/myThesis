package com.thesis.backend.controller;

import com.thesis.backend.service.DeploymentService;
import com.thesis.backend.service.NamespaceService;
import com.thesis.backend.service.PodService;
import com.thesis.backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kubernetes")
public class KubernetesController {

    @Autowired
    private PodService podService;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private NamespaceService namespaceService;

    // Pod management endpoints
    
    @GetMapping("/pods")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<KubernetesPod>> getAllPods(
            @RequestParam(value = "allNamespaces", defaultValue = "false") boolean allNamespaces) {
        return ResponseEntity.ok(podService.getAllPods(allNamespaces));
    }

    @GetMapping("/namespaces/{namespace}/pods")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<List<KubernetesPod>> getPodsInNamespace(
            @PathVariable String namespace) {
        return ResponseEntity.ok(podService.getPodsInNamespace(namespace));
    }

    @GetMapping("/namespaces/{namespace}/pods/{name}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<KubernetesPod> getPod(
            @PathVariable String namespace,
            @PathVariable String name) {
        KubernetesPod pod = podService.getPod(namespace, name);
        if (pod == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pod);
    }

    @PostMapping("/namespaces/{namespace}/pods")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<KubernetesPod> createPod(
            @PathVariable String namespace,
            @RequestParam String name,
            @RequestParam String image,
            @RequestParam(required = false) Map<String, String> labels,
            @RequestParam(required = false) Map<String, String> resources) {
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(podService.createPod(namespace, name, image, labels, resources));
    }

    @DeleteMapping("/namespaces/{namespace}/pods/{name}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deletePod(
            @PathVariable String namespace,
            @PathVariable String name) {
        boolean deleted = podService.deletePod(namespace, name);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/namespaces/{namespace}/pods/{name}/resources")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<KubernetesPod> updatePodResources(
            @PathVariable String namespace,
            @PathVariable String name,
            @RequestBody Map<String, String> resources) {
        
        KubernetesPod pod = podService.updatePodResources(namespace, name, resources);
        if (pod == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pod);
    }

    // Deployment management endpoints
    
    @GetMapping("/deployments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<KubernetesDeployment>> getAllDeployments(
            @RequestParam(value = "allNamespaces", defaultValue = "false") boolean allNamespaces) {
        return ResponseEntity.ok(deploymentService.getAllDeployments(allNamespaces));
    }

    @GetMapping("/namespaces/{namespace}/deployments")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<List<KubernetesDeployment>> getDeploymentsInNamespace(
            @PathVariable String namespace) {
        return ResponseEntity.ok(deploymentService.getDeploymentsInNamespace(namespace));
    }

    @GetMapping("/namespaces/{namespace}/deployments/{name}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<KubernetesDeployment> getDeployment(
            @PathVariable String namespace,
            @PathVariable String name) {
        KubernetesDeployment deployment = deploymentService.getDeployment(namespace, name);
        if (deployment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deployment);
    }

    @PostMapping("/namespaces/{namespace}/deployments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<KubernetesDeployment> createDeployment(
            @PathVariable String namespace,
            @RequestParam String name,
            @RequestParam String image,
            @RequestParam(defaultValue = "1") int replicas,
            @RequestParam(required = false) Map<String, String> labels,
            @RequestParam(required = false) Map<String, String> resources) {
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deploymentService.createDeployment(namespace, name, image, replicas, labels, resources));
    }

    @DeleteMapping("/namespaces/{namespace}/deployments/{name}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteDeployment(
            @PathVariable String namespace,
            @PathVariable String name) {
        boolean deleted = deploymentService.deleteDeployment(namespace, name);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/namespaces/{namespace}/deployments/{name}/scale")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<KubernetesDeployment> scaleDeployment(
            @PathVariable String namespace,
            @PathVariable String name,
            @RequestParam int replicas) {
        
        KubernetesDeployment deployment = deploymentService.scaleDeployment(namespace, name, replicas);
        if (deployment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deployment);
    }

    @PatchMapping("/namespaces/{namespace}/deployments/{name}/image")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<KubernetesDeployment> updateDeploymentImage(
            @PathVariable String namespace,
            @PathVariable String name,
            @RequestParam String image) {
        
        KubernetesDeployment deployment = deploymentService.updateDeploymentImage(namespace, name, image);
        if (deployment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deployment);
    }

    // Namespace management endpoints
    
    @GetMapping("/namespaces")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<List<KubernetesNamespace>> getAllNamespaces() {
        return ResponseEntity.ok(namespaceService.getAllNamespaces());
    }

    @GetMapping("/namespaces/{name}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<KubernetesNamespace> getNamespace(@PathVariable String name) {
        KubernetesNamespace namespace = namespaceService.getNamespace(name);
        if (namespace == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(namespace);
    }

    @PostMapping("/namespaces")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<KubernetesNamespace> createNamespace(
            @RequestParam String name,
            @RequestParam(required = false) Map<String, String> labels) {
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(namespaceService.createNamespace(name, labels));
    }

    @DeleteMapping("/namespaces/{name}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteNamespace(@PathVariable String name) {
        boolean deleted = namespaceService.deleteNamespace(name);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}