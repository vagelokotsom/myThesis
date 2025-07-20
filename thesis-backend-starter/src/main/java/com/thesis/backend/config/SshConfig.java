package com.thesis.backend.config;

import com.thesis.backend.service.SshConnectionService;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.PublicKey;

@Configuration
public class SshConfig {

    private static final Logger logger = LoggerFactory.getLogger(SshConfig.class);

    @Value("${ssh.server.port:2222}")
    private int sshPort;

    @Value("${ssh.server.host:0.0.0.0}")
    private String sshHost;

    @Value("${ssh.server.hostkey.path:hostkey.ser}")
    private String hostKeyPath;

    @Value("${ssh.server.enabled:false}")
    private boolean sshServerEnabled;

    @Autowired
    private SshConnectionService sshConnectionService;

    @Bean
    public SshServer sshServer() throws IOException {
        if (!sshServerEnabled) {
            logger.info("SSH server is disabled");
            return null;
        }

        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost(sshHost);
        sshServer.setPort(sshPort);
        
        // Use a simple host key provider
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(hostKeyPath)));
        
        // Set up password authentication
        sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                logger.info("SSH password authentication attempt for user: {}", username);
                return sshConnectionService.authenticateSshUser(username, password);
            }
        });
        
        // Set up public key authentication (optional)
        sshServer.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            @Override
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                logger.info("SSH public key authentication attempt for user: {}", username);
                // For now, we'll rely on password authentication
                // In future, you could implement public key authentication
                return false;
            }
        });

        // Set up shell factory to provide bash shell
        sshServer.setShellFactory(new ProcessShellFactory("/bin/bash", "-i", "-l"));
        
        // Start the server
        try {
            sshServer.start();
            logger.info("SSH server started on {}:{}", sshHost, sshPort);
        } catch (IOException e) {
            logger.error("Failed to start SSH server", e);
        }

        return sshServer;
    }
}
