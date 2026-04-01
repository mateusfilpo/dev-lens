package br.com.filpo.devlens.server.model;

import java.util.List;

public record DependencyReport(
        String projectPath,
        String pomLocation,
        int totalDependencies,
        int outdatedCount,
        int vulnerableCount,
        List<DependencyInfo> dependencies,
        List<DependencyInfo> outdatedDependencies,
        List<DependencyInfo> vulnerableDependencies,
        String unusedAnalysisNote) {
}