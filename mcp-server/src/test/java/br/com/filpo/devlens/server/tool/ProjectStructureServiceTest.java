package br.com.filpo.devlens.server.tool;

import br.com.filpo.devlens.server.model.ArchitecturePattern;
import br.com.filpo.devlens.server.model.ProjectStructureReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectStructureServiceTest {

    private ProjectStructureService service;

    @BeforeEach
    void setUp() {
        service = new ProjectStructureService();
    }

    @Test
    void shouldDetectHexagonalArchitecture(@TempDir Path tempDir) throws IOException {
        createFakeJavaFile(tempDir, "src/main/java/com/example/domain/port/in", "UseCase.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/domain/port/out", "RepositoryPort.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/infrastructure/adapter/in/web", "Controller.java");

        ProjectStructureReport report = service.readProjectStructure(tempDir.toString());

        assertEquals(ArchitecturePattern.HEXAGONAL, report.detectedPattern());
        assertFalse(report.multiModule());
    }

    @Test
    void shouldDetectMvcArchitecture(@TempDir Path tempDir) throws IOException {
        createFakeJavaFile(tempDir, "src/main/java/com/example/controller", "UserController.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/service", "UserService.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/repository", "UserRepository.java");

        ProjectStructureReport report = service.readProjectStructure(tempDir.toString());

        assertEquals(ArchitecturePattern.MVC, report.detectedPattern());
    }

    @Test
    void shouldDetectCleanArchitecture(@TempDir Path tempDir) throws IOException {
        createFakeJavaFile(tempDir, "src/main/java/com/example/presentation", "View.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/domain", "Entity.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/data", "Dao.java");

        ProjectStructureReport report = service.readProjectStructure(tempDir.toString());

        assertEquals(ArchitecturePattern.CLEAN, report.detectedPattern());
    }

    @Test
    void shouldHandleMultiModuleProject(@TempDir Path tempDir) throws IOException {
        createFakeJavaFile(tempDir, "module-a/src/main/java/com/example/controller", "Ctrl.java");
        createFakeJavaFile(tempDir, "module-a/src/main/java/com/example/service", "Svc.java");
        createFakeJavaFile(tempDir, "module-a/src/main/java/com/example/repository", "Repo.java");
        createFakeJavaFile(tempDir, "module-b/src/main/java/com/example/util", "Util.java");

        ProjectStructureReport report = service.readProjectStructure(tempDir.toString());

        assertTrue(report.multiModule());
        assertTrue(report.modules().contains("module-a"));
        assertTrue(report.modules().contains("module-b"));
        assertEquals(ArchitecturePattern.MVC, report.detectedPattern());
    }

    @Test
    void shouldReturnUnknownForUnrecognizedStructure(@TempDir Path tempDir) throws IOException {
        createFakeJavaFile(tempDir, "src/main/java/com/example/random", "RandomClass.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/util", "Helper.java");

        ProjectStructureReport report = service.readProjectStructure(tempDir.toString());

        assertEquals(ArchitecturePattern.UNKNOWN, report.detectedPattern());
    }

    @Test
    void shouldHandleSingleModuleProject(@TempDir Path tempDir) throws IOException {
        createFakeJavaFile(tempDir, "src/main/java/com/example/controller", "Controller.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/service", "Service.java");
        createFakeJavaFile(tempDir, "src/main/java/com/example/repository", "Repository.java");

        ProjectStructureReport report = service.readProjectStructure(tempDir.toString());

        assertFalse(report.multiModule());
        assertEquals(1, report.modules().size());
        assertTrue(report.modules().contains("."));
        assertEquals(".", report.packages().get(0).moduleName());
        assertEquals(ArchitecturePattern.MVC, report.detectedPattern());
    }

    private void createFakeJavaFile(Path baseDir, String packagePath, String fileName) throws IOException {
        Path fullDirPath = baseDir.resolve(packagePath);
        Files.createDirectories(fullDirPath);
        Files.createFile(fullDirPath.resolve(fileName));
    }
}