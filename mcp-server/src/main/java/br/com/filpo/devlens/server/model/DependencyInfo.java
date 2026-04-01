package br.com.filpo.devlens.server.model;

import java.util.List;

public record DependencyInfo(
        String groupId,
        String artifactId,
        String currentVersion,
        String latestVersion,
        boolean outdated,
        String scope,
        List<VulnerabilityInfo> vulnerabilities) {
}