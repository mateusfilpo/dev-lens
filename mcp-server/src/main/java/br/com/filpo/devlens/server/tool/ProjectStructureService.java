package br.com.filpo.devlens.server.tool;

import br.com.filpo.devlens.server.model.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProjectStructureService {

    @Tool(name = "read_project_structure", description = "Percorre o diretório de um projeto Java e retorna a estrutura de pacotes com detecção do padrão arquitetural utilizado (Hexagonal, MVC ou Clean Architecture)")
    public ProjectStructureReport readProjectStructure(
            @ToolParam(description = "Caminho absoluto do diretório raiz do projeto Java") String projectPath) {

        Path rootPath = Paths.get(projectPath);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("Caminho inválido ou não é um diretório: " + projectPath);
        }

        List<PackageInfo> allPackages = new ArrayList<>();
        Set<String> modules = new HashSet<>();

        try (Stream<Path> paths = Files.walk(rootPath)) {
            List<Path> srcMainJavaDirs = paths
                    .filter(Files::isDirectory)
                    .filter(p -> p.endsWith(Paths.get("src", "main", "java")))
                    .toList();

            for (Path srcDir : srcMainJavaDirs) {
                Path modulePath = rootPath.relativize(srcDir.getParent().getParent().getParent());
                String moduleName = modulePath.toString().isEmpty() ? "." : modulePath.toString();
                modules.add(moduleName);

                allPackages.addAll(scanPackages(srcDir, moduleName));
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler estrutura do projeto: " + e.getMessage(), e);
        }

        boolean multiModule = modules.size() > 1;
        List<String> modulesList = new ArrayList<>(modules);
        ArchitecturePattern pattern = detectPattern(allPackages);

        String justification = generateJustification(pattern, allPackages);

        int totalJavaFiles = allPackages.stream().mapToInt(PackageInfo::fileCount).sum();

        return new ProjectStructureReport(
                projectPath,
                multiModule,
                modulesList,
                pattern,
                justification,
                allPackages,
                allPackages.size(),
                totalJavaFiles);
    }

    private List<PackageInfo> scanPackages(Path srcMainJavaDir, String moduleName) throws IOException {
        List<PackageInfo> packages = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(srcMainJavaDir)) {
            List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (Path dir : dirs) {
                List<String> javaFiles = new ArrayList<>();
                try (Stream<Path> files = Files.list(dir)) {
                    files.filter(f -> f.toString().endsWith(".java"))
                            .forEach(f -> javaFiles.add(f.getFileName().toString()));
                }

                if (!javaFiles.isEmpty()) {
                    Path relativeToSrc = srcMainJavaDir.relativize(dir);
                    String packageName = relativeToSrc.toString().replace(dir.getFileSystem().getSeparator(), ".");

                    String relativePath = moduleName.equals(".") ? relativeToSrc.toString().replace("\\", "/")
                            : moduleName.replace("\\", "/") + "/" + relativeToSrc.toString().replace("\\", "/");

                    packages.add(new PackageInfo(packageName, relativePath, javaFiles, javaFiles.size(), moduleName));
                }
            }
        }
        return packages;
    }

    private ArchitecturePattern detectPattern(List<PackageInfo> packages) {
        Set<String> packageNames = packages.stream()
                .map(PackageInfo::packageName)
                .collect(Collectors.toSet());

        boolean hasPort = packageNames.stream()
                .anyMatch(p -> p.contains(".port.") || p.contains(".ports.") || p.endsWith(".port")
                        || p.endsWith(".ports"));
        boolean hasAdapter = packageNames.stream()
                .anyMatch(p -> p.contains(".adapter.") || p.contains(".adapters.") || p.endsWith(".adapter")
                        || p.endsWith(".adapters"));
        boolean hasDomain = packageNames.stream()
                .anyMatch(p -> p.contains(".domain.") || p.endsWith(".domain"));

        if (hasPort && hasAdapter && hasDomain)
            return ArchitecturePattern.HEXAGONAL;

        boolean hasPresentation = packageNames.stream()
                .anyMatch(p -> p.contains(".presentation.") || p.endsWith(".presentation"));
        boolean hasData = packageNames.stream()
                .anyMatch(p -> p.contains(".data.") || p.endsWith(".data"));

        if (hasPresentation && hasDomain && hasData)
            return ArchitecturePattern.CLEAN;

        boolean hasController = packageNames.stream()
                .anyMatch(p -> p.contains(".controller.") || p.endsWith(".controller"));
        boolean hasService = packageNames.stream().anyMatch(p -> p.contains(".service.") || p.endsWith(".service"));
        boolean hasRepository = packageNames.stream()
                .anyMatch(p -> p.contains(".repository.") || p.endsWith(".repository"));

        if (hasController && hasService && hasRepository)
            return ArchitecturePattern.MVC;

        return ArchitecturePattern.UNKNOWN;
    }

    private String generateJustification(ArchitecturePattern pattern, List<PackageInfo> packages) {
        if (pattern == ArchitecturePattern.UNKNOWN) {
            return "Nenhum padrão arquitetural reconhecido foi detectado nas convenções de pacotes.";
        }

        StringBuilder justification = new StringBuilder();
        justification.append("Padrão ").append(pattern.name()).append(" detectado. Evidências:\n");

        for (PackageInfo pkg : packages) {
            String pName = pkg.packageName();
            String prefix = " - [" + pkg.moduleName() + "] ";

            if (pattern == ArchitecturePattern.HEXAGONAL) {
                if (pName.contains(".port.") || pName.contains(".ports.") || pName.endsWith(".port")
                        || pName.endsWith(".ports"))
                    justification.append(prefix).append("Pacote de port: ").append(pName).append("\n");
                if (pName.contains(".adapter.") || pName.contains(".adapters.") || pName.endsWith(".adapter")
                        || pName.endsWith(".adapters"))
                    justification.append(prefix).append("Pacote de adapter: ").append(pName).append("\n");
                if (pName.contains(".domain.") || pName.endsWith(".domain"))
                    justification.append(prefix).append("Pacote de domínio: ").append(pName).append("\n");
            } else if (pattern == ArchitecturePattern.CLEAN) {
                if (pName.contains(".presentation.") || pName.endsWith(".presentation"))
                    justification.append(prefix).append("Pacote de presentation: ").append(pName).append("\n");
                if (pName.contains(".domain.") || pName.endsWith(".domain"))
                    justification.append(prefix).append("Pacote de domínio: ").append(pName).append("\n");
                if (pName.contains(".data.") || pName.endsWith(".data"))
                    justification.append(prefix).append("Pacote de data: ").append(pName).append("\n");
            } else if (pattern == ArchitecturePattern.MVC) {
                if (pName.contains(".controller.") || pName.endsWith(".controller"))
                    justification.append(prefix).append("Pacote de controller: ").append(pName).append("\n");
                if (pName.contains(".service.") || pName.endsWith(".service"))
                    justification.append(prefix).append("Pacote de service: ").append(pName).append("\n");
                if (pName.contains(".repository.") || pName.endsWith(".repository"))
                    justification.append(prefix).append("Pacote de repository: ").append(pName).append("\n");
            }
        }

        return justification.toString().trim();
    }
}