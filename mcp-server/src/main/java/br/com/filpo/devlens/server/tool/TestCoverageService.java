package br.com.filpo.devlens.server.tool;

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
import java.util.ArrayList;
import java.util.List;

@Service
public class TestCoverageService {

    @Tool(name = "check_test_coverage", description = "Lê o relatório XML do JaCoCo de um projeto Java e retorna métricas de cobertura de testes. Inclui cobertura geral (instrução, branch, linha), cobertura por pacote e por classe, e identifica classes abaixo do threshold.")
    public CoverageReport checkTestCoverage(
            @ToolParam(description = "Caminho absoluto do diretório raiz do projeto Java") String projectPath,
            @ToolParam(description = "Threshold mínimo de cobertura de linha em % (padrão: 80.0)", required = false) Double threshold,
            @ToolParam(description = "Caminho customizado do jacoco.xml. Se não informado, busca em target/site/jacoco/jacoco.xml", required = false) String reportPath) {

        double actualThreshold = (threshold != null) ? threshold : 80.0;

        Path xmlPath;
        if (reportPath != null && !reportPath.isBlank()) {
            xmlPath = Paths.get(reportPath);
        } else {
            xmlPath = Paths.get(projectPath, "target", "site", "jacoco", "jacoco.xml");
        }

        if (!Files.exists(xmlPath) || !Files.isRegularFile(xmlPath)) {
            throw new IllegalArgumentException("Relatório JaCoCo não encontrado em: " + xmlPath
                    + ". Execute 'mvn test jacoco:report' no projeto alvo.");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Evita que o parser tente baixar o DTD da internet (essencial para funcionar
            // offline)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlPath.toFile());
            Element reportElement = doc.getDocumentElement();

            CoverageCounter overallInstruction = extractDirectCounter(reportElement, "INSTRUCTION");
            CoverageCounter overallBranch = extractDirectCounter(reportElement, "BRANCH");
            CoverageCounter overallLine = extractDirectCounter(reportElement, "LINE");

            List<PackageCoverage> packages = new ArrayList<>();
            List<ClassCoverage> allClasses = new ArrayList<>();

            NodeList packageNodes = reportElement.getChildNodes();
            for (int i = 0; i < packageNodes.getLength(); i++) {
                Node pkgNode = packageNodes.item(i);
                if (pkgNode.getNodeType() == Node.ELEMENT_NODE && "package".equals(pkgNode.getNodeName())) {
                    Element pkgElement = (Element) pkgNode;
                    String packageName = pkgElement.getAttribute("name").replace('/', '.');

                    CoverageCounter pkgInstruction = extractDirectCounter(pkgElement, "INSTRUCTION");
                    CoverageCounter pkgBranch = extractDirectCounter(pkgElement, "BRANCH");
                    CoverageCounter pkgLine = extractDirectCounter(pkgElement, "LINE");

                    List<ClassCoverage> classesInPkg = new ArrayList<>();
                    NodeList classNodes = pkgElement.getChildNodes();

                    for (int j = 0; j < classNodes.getLength(); j++) {
                        Node classNode = classNodes.item(j);
                        if (classNode.getNodeType() == Node.ELEMENT_NODE && "class".equals(classNode.getNodeName())) {
                            Element classElement = (Element) classNode;
                            String className = classElement.getAttribute("name").replace('/', '.');
                            String sourceFile = classElement.getAttribute("sourcefilename");

                            CoverageCounter classInstruction = extractDirectCounter(classElement, "INSTRUCTION");
                            CoverageCounter classBranch = extractDirectCounter(classElement, "BRANCH");
                            CoverageCounter classLine = extractDirectCounter(classElement, "LINE");

                            boolean belowThreshold = classLine.percentage() < actualThreshold;
                            ClassCoverage classCov = new ClassCoverage(className, sourceFile, classInstruction,
                                    classBranch, classLine, belowThreshold);

                            classesInPkg.add(classCov);
                            allClasses.add(classCov);
                        }
                    }
                    packages.add(new PackageCoverage(packageName, pkgInstruction, pkgBranch, pkgLine, classesInPkg));
                }
            }

            List<ClassCoverage> belowThresholdList = allClasses.stream().filter(ClassCoverage::belowThreshold).toList();
            int totalClasses = allClasses.size();
            int belowCount = belowThresholdList.size();
            int aboveCount = totalClasses - belowCount;

            String summary = generateSummary(overallLine, belowThresholdList, totalClasses, actualThreshold);

            return new CoverageReport(
                    projectPath,
                    xmlPath.toString(),
                    actualThreshold,
                    overallInstruction,
                    overallBranch,
                    overallLine,
                    packages,
                    belowThresholdList,
                    totalClasses,
                    aboveCount,
                    belowCount,
                    summary);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar relatório JaCoCo: " + e.getMessage(), e);
        }
    }

    private CoverageCounter extractDirectCounter(Element parent, String counterType) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "counter".equals(node.getNodeName())) {
                Element counter = (Element) node;
                if (counterType.equals(counter.getAttribute("type"))) {
                    int missed = Integer.parseInt(counter.getAttribute("missed"));
                    int covered = Integer.parseInt(counter.getAttribute("covered"));
                    return CoverageCounter.of(counterType, missed, covered);
                }
            }
        }
        return CoverageCounter.of(counterType, 0, 0);
    }

    private String generateSummary(CoverageCounter overallLine, List<ClassCoverage> belowThreshold, int totalClasses,
            double threshold) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.US, "Cobertura geral de %.1f%% (linhas). ", overallLine.percentage()));
        sb.append(String.format(java.util.Locale.US, "%d de %d classes abaixo do threshold de %.1f%%.",
                belowThreshold.size(), totalClasses, threshold));

        if (!belowThreshold.isEmpty()) {
            sb.append(" Classes abaixo: ");
            List<String> badClasses = belowThreshold.stream()
                    .map(c -> String.format(java.util.Locale.US, "%s (%.1f%%)",
                            c.className().substring(c.className().lastIndexOf('.') + 1), c.line().percentage()))
                    .toList();
            sb.append(String.join(", ", badClasses));
        }
        return sb.toString();
    }
}