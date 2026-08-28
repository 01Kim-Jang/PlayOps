package com.playops.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "playops")
public record PlayOpsProperties(
        String projectsRoot,
        String storageRoot,
        boolean dockerEnabled,
        Security security
) {
    public record Security(String encryptionKey) {}
}
