package br.com.filpo.devlens.server.tool;

import br.com.filpo.devlens.server.client.MavenCentralClient;
import br.com.filpo.devlens.server.client.OsvClient;
import br.com.filpo.devlens.server.model.DependencyReport;
import br.com.filpo.devlens.server.model.VulnerabilityInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DependencyAnalysisServiceTest {

    private MavenCentralClient mavenCentralClient;
    private OsvClient osvClient;
    private DependencyAnalysisService service;

    @BeforeEach
    void setUp() {
        mavenCentralClient = Mockito.mock(MavenCentralClient.class);
        osvClient = Mockito.mock(OsvClient.class);
        service = new DependencyAnalysisService(mavenCentralClient, osvClient);
    }

    @Test
    void shouldParsePomAndExtractDependencies(@TempDir Path tempDir) throws IOException {
        createFakePom(tempDir, """
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.30</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """);

        when(mavenCentralClient.fetchLatestVersion(anyString(), anyString())).thenReturn("1.18.30");
        when(osvClient.queryBatch(any())).thenReturn(Map.of());

        DependencyReport report = service.analyzeDependencies(tempDir.toString());

        assertEquals(1, report.totalDependencies());
        assertEquals(0, report.outdatedCount());
        assertEquals("provided", report.dependencies().get(0).scope());
    }

    @Test
    void shouldHandleMissingPomGracefully(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> service.analyzeDependencies(tempDir.toString()));
    }

    @Test
    void shouldIdentifyOutdatedDependency(@TempDir Path tempDir) throws IOException {
        createFakePom(tempDir, """
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>31.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        when(mavenCentralClient.fetchLatestVersion(anyString(), anyString())).thenReturn("33.0.0-jre");
        when(osvClient.queryBatch(any())).thenReturn(Map.of());

        DependencyReport report = service.analyzeDependencies(tempDir.toString());

        assertTrue(report.dependencies().get(0).outdated());
        assertEquals(1, report.outdatedCount());
    }

    @Test
    void shouldIdentifyVulnerableDependency(@TempDir Path tempDir) throws IOException {
        createFakePom(tempDir, """
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-core</artifactId>
                            <version>2.14.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        when(mavenCentralClient.fetchLatestVersion(anyString(), anyString())).thenReturn("2.14.1");
        when(osvClient.queryBatch(any())).thenReturn(Map.of(
                "org.apache.logging.log4j:log4j-core",
                List.of(new VulnerabilityInfo("CVE-2021-44228", "Log4Shell", "CRITICAL", "", ""))));

        DependencyReport report = service.analyzeDependencies(tempDir.toString());

        assertEquals(1, report.vulnerableCount());
        assertEquals("CVE-2021-44228", report.vulnerableDependencies().get(0).vulnerabilities().get(0).id());
    }

    @Test
    void shouldResolvePropertyPlaceholders(@TempDir Path tempDir) throws IOException {
        createFakePom(tempDir, """
                <project>
                    <properties>
                        <guava.version>32.0.0-jre</guava.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${guava.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        when(mavenCentralClient.fetchLatestVersion(anyString(), anyString())).thenReturn("32.0.0-jre");

        DependencyReport report = service.analyzeDependencies(tempDir.toString());
        assertEquals("32.0.0-jre", report.dependencies().get(0).currentVersion());
    }

    private void createFakePom(Path dir, String content) throws IOException {
        Files.writeString(dir.resolve("pom.xml"), content);
    }
}