package com.thesis.backend.service;

import com.thesis.backend.entity.User;
import io.fabric8.kubernetes.api.model.authentication.TokenRequest;
import io.fabric8.kubernetes.api.model.authentication.TokenRequestBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Collections;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubeconfigService {

    private static final Path CA_CERT_PATH = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");

    @Value("${app.kubeconfig.server-url:}")
    private String kubeconfigServerUrl;

    @Value("${app.kubeconfig.minikube-api-port:8443}")
    private int minikubeApiPort;

    private final KubernetesClient kubernetesClient;
    private final StudentNamespaceService studentNamespaceService;

    public String buildKubeconfig(User user) {
        String namespace = studentNamespaceService.ensureStudentNamespace(user, null, "standard", null);
        String serviceAccountName = String.format("student-%d-sa", user.getId());

        String server = resolveApiServer();

        List<String> audiences = new ArrayList<>();
        audiences.add("https://kubernetes.default.svc");
        audiences.add("https://kubernetes.default.svc.cluster.local");
        if (server != null && !server.isBlank()) {
            audiences.add(server);
        }
        TokenRequest tokenRequest = new TokenRequestBuilder()
                .withNewSpec()
                    .withAudiences(audiences)
                    .withExpirationSeconds(86400L) // 24 hours.
                .endSpec()
                .build();

        TokenRequest token = kubernetesClient.serviceAccounts()
                .inNamespace(namespace)
                .withName(serviceAccountName)
                .tokenRequest(tokenRequest);

        String tokenValue = token.getStatus().getToken();
        String apiServer = server != null ? server : normalizeUrl(kubernetesClient.getConfiguration().getMasterUrl());

        String caData = "";
        try {
            byte[] caBytes = Files.readAllBytes(CA_CERT_PATH);
            caData = Base64.getEncoder().encodeToString(caBytes);
        } catch (Exception e) {
            log.warn("Unable to read cluster CA certificate: {}", e.getMessage());
        }

        String clusterName = "kubernetes";
        String userName = user.getUsername();
        String contextName = userName + "@" + clusterName;

        return String.format(
                "apiVersion: v1%n" +
                "kind: Config%n" +
                "clusters:%n" +
                "- name: %s%n" +
                "  cluster:%n" +
                "    server: %s%n" +
                "    certificate-authority-data: %s%n" +
                "users:%n" +
                "- name: %s%n" +
                "  user:%n" +
                "    token: %s%n" +
                "contexts:%n" +
                "- name: %s%n" +
                "  context:%n" +
                "    cluster: %s%n" +
                "    user: %s%n" +
                "    namespace: %s%n" +
                "current-context: %s%n",
                clusterName,
                apiServer,
                caData,
                userName,
                tokenValue,
                contextName,
                clusterName,
                userName,
                namespace,
                contextName
        );
    }

    private String resolveApiServer() {
        String configured = normalizeUrl(kubeconfigServerUrl);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        String discovered = normalizeUrl(kubernetesClient.getConfiguration().getMasterUrl());
        if (!isClusterInternalAddress(discovered)) {
            return discovered;
        }

        String minikubeServer = detectMinikubeServer();
        if (minikubeServer != null) {
            log.info("Using detected minikube API server for kubeconfig: {}", minikubeServer);
            return minikubeServer;
        }

        log.warn("Falling back to in-cluster API server {} for kubeconfig; this may not be reachable from client machines.", discovered);
        return discovered;
    }

    private String detectMinikubeServer() {
        try {
            return kubernetesClient.nodes().list().getItems().stream()
                    .filter(Objects::nonNull)
                    .filter(node -> {
                        String name = node.getMetadata() != null ? node.getMetadata().getName() : "";
                        return name != null && name.toLowerCase().contains("minikube");
                    })
                    .findFirst()
                    .flatMap(node -> {
                        List<io.fabric8.kubernetes.api.model.NodeAddress> addresses =
                                node.getStatus() != null && node.getStatus().getAddresses() != null
                                        ? node.getStatus().getAddresses()
                                        : Collections.emptyList();
                        return addresses.stream()
                            .filter(address -> "InternalIP".equals(address.getType()))
                            .findFirst()
                            .map(address -> "https://" + address.getAddress() + ":" + minikubeApiPort);
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to detect minikube server URL: {}", e.getMessage());
            return null;
        }
    }

    private boolean isClusterInternalAddress(String server) {
        if (server == null || server.isBlank()) return true;
        return server.contains("kubernetes.default.svc")
                || server.contains("kubernetes.default.svc.cluster.local")
                || server.contains("10.96.0.1");
    }

    private String normalizeUrl(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
