package br.com.filpo.devlens.server.tool;

import br.com.filpo.devlens.server.model.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestCoverageServiceTest {

    private TestCoverageService service;

    static final String MINIMAL_JACOCO_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
            <report name="test">
                <package name="com/example">
                    <class name="com/example/MyClass" sourcefilename="MyClass.java">
                        <counter type="INSTRUCTION" missed="5" covered="45"/>
                        <counter type="BRANCH" missed="1" covered="7"/>
                        <counter type="LINE" missed="2" covered="18"/>
                        <counter type="METHOD" missed="0" covered="3"/>
                        <counter type="CLASS" missed="0" covered="1"/>
                    </class>
                    <class name="com/example/BadClass" sourcefilename="BadClass.java">
                        <counter type="LINE" missed="15" covered="5"/>
                    </class>
                    <counter type="INSTRUCTION" missed="5" covered="45"/>
                    <counter type="BRANCH" missed="1" covered="7"/>
                    <counter type="LINE" missed="17" covered="23"/>
                </package>
                <counter type="INSTRUCTION" missed="20" covered="80"/>
                <counter type="BRANCH" missed="5" covered="15"/>
                <counter type="LINE" missed="10" covered="40"/>
            </report>
            """;

    @BeforeEach
    void setUp() {
        service = new TestCoverageService();
    }

    private Path createFakeJacocoReport(Path projectDir, String xmlContent) throws IOException {
        Path jacocoDir = projectDir.resolve("target/site/jacoco");
        Files.createDirectories(jacocoDir);
        Path reportFile = jacocoDir.resolve("jacoco.xml");
        Files.writeString(reportFile, xmlContent);
        return reportFile;
    }

    @Test
    void shouldParseOverallCoverage(@TempDir Path tempDir) throws IOException {
        createFakeJacocoReport(tempDir, MINIMAL_JACOCO_XML);

        CoverageReport report = service.checkTestCoverage(tempDir.toString(), null, null);

        assertEquals(80.0, report.overallInstruction().percentage(), 0.1);
        assertEquals(75.0, report.overallBranch().percentage(), 0.1);
        assertEquals(80.0, report.overallLine().percentage(), 0.1);
    }

    @Test
    void shouldExtractPackageNames(@TempDir Path tempDir) throws IOException {
        createFakeJacocoReport(tempDir, MINIMAL_JACOCO_XML);

        CoverageReport report = service.checkTestCoverage(tempDir.toString(), null, null);

        assertEquals(1, report.packages().size());
        assertEquals("com.example", report.packages().get(0).packageName());
    }

    @Test
    void shouldIdentifyClassBelowThreshold(@TempDir Path tempDir) throws IOException {
        createFakeJacocoReport(tempDir, MINIMAL_JACOCO_XML);

        // Threshold default é 80. BadClass tem 5/20 = 25% de cobertura. MyClass tem
        // 18/20 = 90%
        CoverageReport report = service.checkTestCoverage(tempDir.toString(), 80.0, null);

        assertEquals(1, report.classesBelowThresholdCount());
        assertTrue(report.classesBelowThreshold().get(0).className().contains("BadClass"));
        assertTrue(report.classesBelowThreshold().get(0).belowThreshold());
        assertEquals(2, report.totalClasses());
        assertEquals(1, report.classesAboveThreshold());
    }

    @Test
    void shouldUseDefaultThresholdWhenNull(@TempDir Path tempDir) throws IOException {
        createFakeJacocoReport(tempDir, MINIMAL_JACOCO_XML);

        CoverageReport report = service.checkTestCoverage(tempDir.toString(), null, null);

        assertEquals(80.0, report.coverageThreshold());
    }

    @Test
    void shouldHandleMissingReportGracefully(@TempDir Path tempDir) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.checkTestCoverage(tempDir.toString(), null, null));
        assertTrue(exception.getMessage().contains("mvn test jacoco:report"));
    }

    @Test
    void shouldHandleCustomReportPath(@TempDir Path tempDir) throws IOException {
        Path customDir = tempDir.resolve("custom-report");
        Files.createDirectories(customDir);
        Path customFile = customDir.resolve("my-jacoco.xml");
        Files.writeString(customFile, MINIMAL_JACOCO_XML);

        CoverageReport report = service.checkTestCoverage(tempDir.toString(), null, customFile.toString());

        assertNotNull(report);
        assertEquals(80.0, report.overallLine().percentage(), 0.1);
    }

    @Test
    void shouldGenerateNonEmptySummary(@TempDir Path tempDir) throws IOException {
        createFakeJacocoReport(tempDir, MINIMAL_JACOCO_XML);

        CoverageReport report = service.checkTestCoverage(tempDir.toString(), 80.0, null);

        assertNotNull(report.summary());
        assertFalse(report.summary().isBlank());
        assertTrue(report.summary().contains("Cobertura geral de 80.0%"));
        assertTrue(report.summary().contains("BadClass (25.0%)"));
    }
}