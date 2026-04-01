package br.com.filpo.devlens.server.tool;

import br.com.filpo.devlens.server.client.MavenCentralClient;
import br.com.filpo.devlens.server.client.OsvClient;
import br.com.filpo.devlens.server.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class DependencyAnalysisService {

    private final MavenCentralClient mavenCentralClient;
    private final OsvClient osvClient;

    public DependencyAnalysisService(MavenCentralClient mavenCentralClient, OsvClient osvClient) {
        this.mavenCentralClient = mavenCentralClient;
        this.osvClient = osvClient;
    }

    @Tool(name = "analyze_dependencies", description = "Lê o pom.xml de um projeto Java e analisa as dependências declaradas. "
            +
            "Identifica dependências desatualizadas comparando com Maven Central " +
            "e verifica vulnerabilidades conhecidas (CVEs) via OSV.dev")
    public DependencyReport analyzeDependencies(
            @ToolParam(description = "Caminho absoluto do diretório raiz do projeto Java") String projectPath) {

        Path pomPath = Paths.get(projectPath, "pom.xml");
        if (!Files.exists(pomPath) || !Files.isRegularFile(pomPath)) {
            throw new IllegalArgumentException("pom.xml não encontrado no diretório: " + projectPath);
        }

        List<RawDependency> rawDependencies = parsePom(pomPath);

        List<DependencyInfo> finalDependencies = new ArrayList<>();
        Map<String, String> versionQueryMap = new HashMap<>();

        for (RawDependency raw : rawDependencies) {
            if (raw.version() == null || raw.version().isBlank() || raw.version().startsWith("${")) {
                // Pula dependências sem versão explícita/resolvida para a query do OSV
                finalDependencies.add(new DependencyInfo(raw.groupId(), raw.artifactId(), raw.version(), null, false,
                        raw.scope(), List.of()));
                continue;
            }

            String latestVersion = mavenCentralClient.fetchLatestVersion(raw.groupId(), raw.artifactId());
            boolean outdated = latestVersion != null && !latestVersion.equals(raw.version());

            finalDependencies.add(new DependencyInfo(raw.groupId(), raw.artifactId(), raw.version(), latestVersion,
                    outdated, raw.scope(), new ArrayList<>()));
            versionQueryMap.put(raw.groupId() + ":" + raw.artifactId(), raw.version());
        }

        // Busca vulnerabilidades em batch
        Map<String, List<VulnerabilityInfo>> vulnsMap = osvClient.queryBatch(versionQueryMap);

        List<DependencyInfo> enrichedDependencies = finalDependencies.stream().map(dep -> {
            String key = dep.groupId() + ":" + dep.artifactId();
            List<VulnerabilityInfo> vulns = vulnsMap.getOrDefault(key, List.of());
            return new DependencyInfo(dep.groupId(), dep.artifactId(), dep.currentVersion(), dep.latestVersion(),
                    dep.outdated(), dep.scope(), vulns);
        }).toList();

        List<DependencyInfo> outdatedList = enrichedDependencies.stream().filter(DependencyInfo::outdated).toList();
        List<DependencyInfo> vulnerableList = enrichedDependencies.stream().filter(d -> !d.vulnerabilities().isEmpty())
                .toList();

        return new DependencyReport(
                projectPath,
                "pom.xml",
                enrichedDependencies.size(),
                outdatedList.size(),
                vulnerableList.size(),
                enrichedDependencies,
                outdatedList,
                vulnerableList,
                "Análise de dependências sem uso não implementada — requer execução de 'mvn dependency:analyze' no projeto alvo.");
    }

    private List<RawDependency> parsePom(Path pomPath) {
        List<RawDependency> deps = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(pomPath.toFile());

            Map<String, String> properties = extractProperties(document);
            NodeList dependencyNodes = document.getElementsByTagName("dependency");

            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element elem = (Element) dependencyNodes.item(i);
                String groupId = getTagValue(elem, "groupId");
                String artifactId = getTagValue(elem, "artifactId");
                String version = resolveProperty(getTagValue(elem, "version"), properties);
                String scope = getTagValue(elem, "scope");
                if (scope == null)
                    scope = "compile";

                if (groupId != null && artifactId != null) {
                    deps.add(new RawDependency(groupId, artifactId, version, scope));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao parsear o pom.xml: " + e.getMessage(), e);
        }
        return deps;
    }

    private Map<String, String> extractProperties(Document document) {
        Map<String, String> props = new HashMap<>();
        NodeList propNodes = document.getElementsByTagName("properties");
        if (propNodes.getLength() > 0) {
            NodeList children = propNodes.item(0).getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    props.put(node.getNodeName(), node.getTextContent().trim());
                }
            }
        }
        return props;
    }

    private String getTagValue(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent().trim();
        }
        return null;
    }

    private String resolveProperty(String value, Map<String, String> properties) {
        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String propName = value.substring(2, value.length() - 1);
            return properties.getOrDefault(propName, value);
        }
        return value;
    }

    private record RawDependency(String groupId, String artifactId, String version, String scope) {
    }
}