package com.thesis.backend.service;

import com.thesis.backend.model.KubernetesPod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PodService {

    @Autowired
    private KubernetesClient kubernetesClient;

    /**
     * Get all pods in the current namespace or in all namespaces if specified
     */
    public List<KubernetesPod> getAllPods(boolean allNamespaces) {
        List<Pod> pods;
        if (allNamespaces) {
            pods = kubernetesClient.pods().inAnyNamespace().list().getItems();
        } else {
            pods = kubernetesClient.pods().list().getItems();
        }

        return pods.stream()
                .map(this::mapPodToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get pods in a specific namespace
     */
    public List<KubernetesPod> getPodsInNamespace(String namespace) {
        List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
        return pods.stream()
                .map(this::mapPodToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific pod by name in a namespace
     */
    public KubernetesPod getPod(String namespace, String name) {
        Pod pod = kubernetesClient.pods().inNamespace(namespace).withName(name).get();
        if (pod == null) {
            return null;
        }
        return mapPodToDto(pod);
    }

    /**
     * Create a new pod
     */
    public KubernetesPod createPod(String namespace, String name, String image, 
                                  Map<String, String> labels, Map<String, String> resources) {
        
        Map<String, Quantity> resourceLimits = new HashMap<>();
        Map<String, Quantity> resourceRequests = new HashMap<>();
        
        if (resources.containsKey("cpu-limit")) {
            resourceLimits.put("cpu", new Quantity(resources.get("cpu-limit")));
        }
        if (resources.containsKey("memory-limit")) {
            resourceLimits.put("memory", new Quantity(resources.get("memory-limit")));
        }
        if (resources.containsKey("cpu-request")) {
            resourceRequests.put("cpu", new Quantity(resources.get("cpu-request")));
        }
        if (resources.containsKey("memory-request")) {
            resourceRequests.put("memory", new Quantity(resources.get("memory-request")));
        }

        Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                    .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName(name)
                        .withImage(image)
                        .withNewResources()
                            .withLimits(resourceLimits)
                            .withRequests(resourceRequests)
                        .endResources()
                    .endContainer()
                .endSpec()
                .build();

        Pod createdPod = kubernetesClient.pods().inNamespace(namespace).create(pod);
        return mapPodToDto(createdPod);
    }

    /**
     * Delete a pod
     */
    public boolean deletePod(String namespace, String name) {
        return !kubernetesClient.pods().inNamespace(namespace).withName(name).delete().isEmpty();
    }

    /**
     * Update pod resources
     */
    public KubernetesPod updatePodResources(String namespace, String name, 
                                          Map<String, String> resources) {
        
        PodResource podResource = kubernetesClient.pods().inNamespace(namespace).withName(name);

        
        Pod existingPod = podResource.get();
        if (existingPod == null) {
            return null;
        }

        Map<String, Quantity> resourceLimits = new HashMap<>();
        Map<String, Quantity> resourceRequests = new HashMap<>();
        
        if (resources.containsKey("cpu-limit")) {
            resourceLimits.put("cpu", new Quantity(resources.get("cpu-limit")));
        }
        if (resources.containsKey("memory-limit")) {
            resourceLimits.put("memory", new Quantity(resources.get("memory-limit")));
        }
        if (resources.containsKey("cpu-request")) {
            resourceRequests.put("cpu", new Quantity(resources.get("cpu-request")));
        }
        if (resources.containsKey("memory-request")) {
            resourceRequests.put("memory", new Quantity(resources.get("memory-request")));
        }

        Pod updatedPod = new PodBuilder(existingPod)
                .editSpec()
                    .editContainer(0)
                        .editResources()
                            .withLimits(resourceLimits)
                            .withRequests(resourceRequests)
                        .endResources()
                    .endContainer()
                .endSpec()
                .build();

        Pod result = podResource.replace(updatedPod);
        return mapPodToDto(result);
    }

    /**
     * Map Kubernetes Pod to a simplified DTO
     */
    private KubernetesPod mapPodToDto(Pod pod) {
        KubernetesPod kubernetesPod = new KubernetesPod();
        kubernetesPod.setName(pod.getMetadata().getName());
        kubernetesPod.setNamespace(pod.getMetadata().getNamespace());
        kubernetesPod.setStatus(pod.getStatus().getPhase());
        
        if (pod.getStatus().getPodIP() != null) {
            kubernetesPod.setIp(pod.getStatus().getPodIP());
        }
        
        if (pod.getMetadata().getLabels() != null) {
            kubernetesPod.setLabels(pod.getMetadata().getLabels());
        }
        
        // Extract resource limits/requests if available
        if (pod.getSpec() != null && 
            pod.getSpec().getContainers() != null && 
            !pod.getSpec().getContainers().isEmpty() && 
            pod.getSpec().getContainers().get(0).getResources() != null) {
            
            Map<String, String> resources = new HashMap<>();
            
            // CPU & Memory Limits
            if (pod.getSpec().getContainers().get(0).getResources().getLimits() != null) {
                if (pod.getSpec().getContainers().get(0).getResources().getLimits().containsKey("cpu")) {
                    resources.put("cpu-limit", pod.getSpec().getContainers().get(0).getResources().getLimits().get("cpu").toString());
                }
                if (pod.getSpec().getContainers().get(0).getResources().getLimits().containsKey("memory")) {
                    resources.put("memory-limit", pod.getSpec().getContainers().get(0).getResources().getLimits().get("memory").toString());
                }
            }
            
            // CPU & Memory Requests
            if (pod.getSpec().getContainers().get(0).getResources().getRequests() != null) {
                if (pod.getSpec().getContainers().get(0).getResources().getRequests().containsKey("cpu")) {
                    resources.put("cpu-request", pod.getSpec().getContainers().get(0).getResources().getRequests().get("cpu").toString());
                }
                if (pod.getSpec().getContainers().get(0).getResources().getRequests().containsKey("memory")) {
                    resources.put("memory-request", pod.getSpec().getContainers().get(0).getResources().getRequests().get("memory").toString());
                }
            }
            
            kubernetesPod.setResources(resources);
        }
        
        return kubernetesPod;
    }
}
