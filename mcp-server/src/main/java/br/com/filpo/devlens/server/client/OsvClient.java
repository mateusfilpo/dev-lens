package br.com.filpo.devlens.server.client;

import br.com.filpo.devlens.server.model.VulnerabilityInfo;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class OsvClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OsvClient(@Qualifier("osvRestClient") RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, List<VulnerabilityInfo>> queryBatch(Map<String, String> dependencies) {
        Map<String, List<VulnerabilityInfo>> resultMap = new HashMap<>();
        if (dependencies.isEmpty())
            return resultMap;

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode queries = requestBody.putArray("queries");

            List<String> orderedKeys = new ArrayList<>();

            for (Map.Entry<String, String> entry : dependencies.entrySet()) {
                orderedKeys.add(entry.getKey());
                ObjectNode query = objectMapper.createObjectNode();
                query.put("version", entry.getValue());
                ObjectNode pkg = query.putObject("package");
                pkg.put("name", entry.getKey());
                pkg.put("ecosystem", "Maven");
                queries.add(query);
            }

            JsonNode response = restClient.post()
                    .uri("/v1/querybatch")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("results")) {
                ArrayNode results = (ArrayNode) response.get("results");
                for (int i = 0; i < results.size(); i++) {
                    String depKey = orderedKeys.get(i);
                    List<VulnerabilityInfo> vulnsList = new ArrayList<>();
                    JsonNode resultNode = results.get(i);

                    if (resultNode.has("vulns")) {
                        for (JsonNode vuln : resultNode.get("vulns")) {
                            String id = vuln.has("id") ? vuln.get("id").asString() : "UNKNOWN";
                            String summary = vuln.has("summary") ? vuln.get("summary").asString() : "Sem descrição";
                            String severity = "UNKNOWN";

                            // Tenta buscar o texto de severidade se existir
                            if (vuln.has("database_specific") && vuln.get("database_specific").has("severity")) {
                                severity = vuln.get("database_specific").get("severity").asString();
                            }

                            vulnsList.add(new VulnerabilityInfo(id, summary, severity, "Consulte OSV", "Consulte OSV"));
                        }
                    }
                    resultMap.put(depKey, vulnsList);
                }
            }
        } catch (Exception e) {
            // Retorna vazio em caso de erro na rede
        }
        return resultMap;
    }
}