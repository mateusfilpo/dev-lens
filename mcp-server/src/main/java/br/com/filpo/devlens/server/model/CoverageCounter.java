package br.com.filpo.devlens.server.model;

public record CoverageCounter(
        String type,
        int missed,
        int covered,
        double percentage) {
    public static CoverageCounter of(String type, int missed, int covered) {
        double total = missed + covered;
        double pct = total > 0 ? (covered / total) * 100.0 : 0.0;
        return new CoverageCounter(type, missed, covered, pct);
    }
}