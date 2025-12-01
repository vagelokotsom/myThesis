package com.thesis.backend.service;

import com.thesis.backend.model.KubernetesDeployment;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeploymentService {

    @Autowired
    private KubernetesClient kubernetesClient;

    /**
     * Get all deployments in the current namespace or in all namespaces if specified
     */
    public List<KubernetesDeployment> getAllDeployments(boolean allNamespaces) {
        List<Deployment> deployments;
        if (allNamespaces) {
            deployments = kubernetesClient.apps().deployments().inAnyNamespace().list().getItems();
        } else {
            deployments = kubernetesClient.apps().deployments().list().getItems();
        }

        return deployments.stream()
                .map(this::mapDeploymentToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get deployments in a specific namespace
     */
    public List<KubernetesDeployment> getDeploymentsInNamespace(String namespace) {
        List<Deployment> deployments = kubernetesClient.apps().deployments().inNamespace(namespace).list().getItems();
        return deployments.stream()
                .map(this::mapDeploymentToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific deployment by name in a namespace
     */
    public KubernetesDeployment getDeployment(String namespace, String name) {
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).get();
        if (deployment == null) {
            return null;
        }
        return mapDeploymentToDto(deployment);
    }

    /**
     * Create a new deployment
     */
    public KubernetesDeployment createDeployment(String namespace, String name, String image,
                                                int replicas, Map<String, String> labels,
                                                Map<String, String> resources) {

        Map<String, String> selectorLabels = new HashMap<>();
        selectorLabels.put("app", name);

        if (labels == null) {
            labels = new HashMap<>();
        }
        labels.put("app", name);

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withReplicas(replicas)
                .withNewSelector()
                .withMatchLabels(selectorLabels)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(selectorLabels)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(name)
                .withImage(image)
                .withImagePullPolicy("never")
                .withNewResources()
                .addToLimits(getResourceQuantities(resources, "limit"))
                .addToRequests(getResourceQuantities(resources, "request"))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        Deployment createdDeployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace).resource(deployment).serverSideApply();

        return mapDeploymentToDto(createdDeployment);
    }

    /**
     * Delete a deployment
     */
    public boolean deleteDeployment(String namespace, String name) {
        return !kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).delete().isEmpty();
    }

    /**
     * Scale a deployment
     */
    public KubernetesDeployment scaleDeployment(String namespace, String name, int replicas) {
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace).withName(name).scale(replicas);
        return mapDeploymentToDto(deployment);
    }

    /**
     * Update deployment image
     */
    public KubernetesDeployment updateDeploymentImage(String namespace, String name, String newImage) {
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace).withName(name).get();

        if (deployment == null) {
            return null;
        }

        Deployment updatedDeployment = new DeploymentBuilder(deployment)
                .editSpec()
                .editTemplate()
                .editSpec()
                .editContainer(0)
                .withImage(newImage)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        Deployment result = kubernetesClient.apps().deployments()
                .inNamespace(namespace).resource(updatedDeployment).serverSideApply();

        return mapDeploymentToDto(result);
    }

    /**
     * Map Kubernetes Deployment to a simplified DTO
     */
    private KubernetesDeployment mapDeploymentToDto(Deployment deployment) {
        KubernetesDeployment kubernetesDeployment = new KubernetesDeployment();
        kubernetesDeployment.setName(deployment.getMetadata().getName());
        kubernetesDeployment.setNamespace(deployment.getMetadata().getNamespace());
        kubernetesDeployment.setReplicas(deployment.getSpec().getReplicas());
        kubernetesDeployment.setAvailableReplicas(
                deployment.getStatus() != null ? deployment.getStatus().getAvailableReplicas() : 0);

        if (deployment.getMetadata().getLabels() != null) {
            kubernetesDeployment.setLabels(deployment.getMetadata().getLabels());
        }

        if (deployment.getSpec() != null &&
                deployment.getSpec().getTemplate() != null &&
                deployment.getSpec().getTemplate().getSpec() != null &&
                deployment.getSpec().getTemplate().getSpec().getContainers() != null &&
                !deployment.getSpec().getTemplate().getSpec().getContainers().isEmpty()) {

            kubernetesDeployment.setImage(
                    deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());

            // Extract resource limits/requests if available
            if (deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources() != null) {
                Map<String, String> resources = new HashMap<>();

                // CPU & Memory Limits
                if (deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits() != null) {
                    if (deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().containsKey("cpu")) {
                        resources.put("cpu-limit", deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().get("cpu").toString());
                    }
                    if (deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().containsKey("memory")) {
                        resources.put("memory-limit", deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().get("memory").toString());
                    }
                }

                // CPU & Memory Requests
                if (deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests() != null) {
                    if (deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().containsKey("cpu")) {
                        resources.put("cpu-request", deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().get("cpu").toString());
                    }
                    if (deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().containsKey("memory")) {
                        resources.put("memory-request", deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().get("memory").toString());
                    }
                }

                kubernetesDeployment.setResources(resources);
            }
        }

        return kubernetesDeployment;
    }

    /**
     * Helper method to convert resource strings to Quantity map
     */
    private Map<String, io.fabric8.kubernetes.api.model.Quantity> getResourceQuantities(
            Map<String, String> resources, String type) {
        Map<String, io.fabric8.kubernetes.api.model.Quantity> result = new HashMap<>();

        if (resources != null) {
            if (resources.containsKey("cpu-" + type)) {
                result.put("cpu", new io.fabric8.kubernetes.api.model.Quantity(resources.get("cpu-" + type)));
            }
            if (resources.containsKey("memory-" + type)) {
                result.put("memory", new io.fabric8.kubernetes.api.model.Quantity(resources.get("memory-" + type)));
            }
        }

        return result;
    }
}
