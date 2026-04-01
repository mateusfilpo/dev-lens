package br.com.filpo.devlens.server.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Component
public class MavenCentralClient {

    private final RestClient restClient;

    public MavenCentralClient(@Qualifier("mavenCentralRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public String fetchLatestVersion(String groupId, String artifactId) {
        try {
            String url = String.format("/solrsearch/select?q=g:\"%s\"+AND+a:\"%s\"&wt=json&rows=1", groupId,
                    artifactId);
            JsonNode response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("response")) {
                JsonNode docs = response.get("response").get("docs");
                if (docs.isArray() && !docs.isEmpty()) {
                    return docs.get(0).get("latestVersion").asString();
                }
            }
        } catch (Exception e) {
            // Logar falha silenciosamente (poderia usar Slf4j)
        }
        return null;
    }
}