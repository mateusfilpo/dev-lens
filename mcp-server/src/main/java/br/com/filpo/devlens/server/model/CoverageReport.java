package br.com.filpo.devlens.server.model;

import java.util.List;

public record CoverageReport(
        String projectPath,
        String reportFile,
        double coverageThreshold,
        CoverageCounter overallInstruction,
        CoverageCounter overallBranch,
        CoverageCounter overallLine,
        List<PackageCoverage> packages,
        List<ClassCoverage> classesBelowThreshold,
        int totalClasses,
        int classesAboveThreshold,
        int classesBelowThresholdCount,
        String summary) {
}