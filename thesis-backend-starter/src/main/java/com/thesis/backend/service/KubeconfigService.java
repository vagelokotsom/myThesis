package com.thesis.backend.service;

import com.thesis.backend.entity.User;
import io.fabric8.kubernetes.api.model.authentication.TokenRequest;
import io.fabric8.kubernetes.api.model.authentication.TokenRequestBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubeconfigService {

    private static final Path CA_CERT_PATH = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");

    private final KubernetesClient kubernetesClient;
    private final StudentNamespaceService studentNamespaceService;

    public String buildKubeconfig(User user) {
        String namespace = studentNamespaceService.ensureStudentNamespace(user, null, "standard", null);
        String serviceAccountName = String.format("student-%d-sa", user.getId());

        String server = kubernetesClient.getConfiguration().getMasterUrl();
        if (server != null && server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }

        TokenRequest tokenRequest = new TokenRequestBuilder()
                .withNewSpec()
                    .addToAudiences(
                            "https://kubernetes.default.svc",
                            "https://kubernetes.default.svc.cluster.local",
                            server != null ? server : "")
                    .withExpirationSeconds(86400L) // 1 hour.
                .endSpec()
                .build();

        TokenRequest token = kubernetesClient.serviceAccounts()
                .inNamespace(namespace)
                .withName(serviceAccountName)
                .tokenRequest(tokenRequest);

        String tokenValue = token.getStatus().getToken();
        String apiServer = server != null ? server : kubernetesClient.getConfiguration().getMasterUrl();

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
}
