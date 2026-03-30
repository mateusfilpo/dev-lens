package br.com.filpo.devlens.server.model;

import java.util.List;

public record PackageInfo(
        String packageName,
        String relativePath,
        List<String> javaFiles,
        int fileCount,
        String moduleName) {
}