package br.com.filpo.devlens.server.model;

public enum ArchitecturePattern {
    HEXAGONAL("Hexagonal Architecture (Ports & Adapters)"),
    MVC("Model-View-Controller (MVC)"),
    CLEAN("Clean Architecture"),
    UNKNOWN("Padrão não identificado");

    private final String description;

    ArchitecturePattern(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}