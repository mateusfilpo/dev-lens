package br.com.filpo.devlens.server.model;

import java.util.List;

public record ProjectStructureReport(
        String projectPath,
        boolean multiModule,
        List<String> modules,
        ArchitecturePattern detectedPattern,
        String patternJustification,
        List<PackageInfo> packages,
        int totalPackages,
        int totalJavaFiles) {
}