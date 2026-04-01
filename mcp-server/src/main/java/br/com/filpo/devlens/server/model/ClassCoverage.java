package br.com.filpo.devlens.server.model;

public record ClassCoverage(
        String className,
        String sourceFile,
        CoverageCounter instruction,
        CoverageCounter branch,
        CoverageCounter line,
        boolean belowThreshold) {
}