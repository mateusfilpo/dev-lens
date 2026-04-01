package br.com.filpo.devlens.server.model;

import java.util.List;

public record PackageCoverage(
        String packageName,
        CoverageCounter instruction,
        CoverageCounter branch,
        CoverageCounter line,
        List<ClassCoverage> classes) {
}