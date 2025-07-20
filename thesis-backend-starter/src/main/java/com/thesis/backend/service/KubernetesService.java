
package com.thesis.backend.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.*;
import org.springframework.stereotype.Service;
import io.fabric8.kubernetes.api.model.PodBuilder;


@Service
public class KubernetesService {

    private final KubernetesClient client = new KubernetesClientBuilder().build();

    public String createContainer(String image, String username) {
        String podName = "pod-" + username + "-" + System.currentTimeMillis();
        Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName("example-pod")
                    .withNamespace("default")
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName("example-container")
                        .withImage("nginx")
                    .endContainer()
                .endSpec()
                .build();

        client.pods().inNamespace("default").create(pod);

        return pod.getMetadata().getName();
    }
}
