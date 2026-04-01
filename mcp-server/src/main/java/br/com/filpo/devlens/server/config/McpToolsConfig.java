package br.com.filpo.devlens.server.config;

import br.com.filpo.devlens.server.tool.DependencyAnalysisService;
import br.com.filpo.devlens.server.tool.ProjectStructureService;
import br.com.filpo.devlens.server.tool.TestCoverageService;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public MethodToolCallbackProvider toolCallbackProvider(
            ProjectStructureService projectStructureService,
            DependencyAnalysisService dependencyAnalysisService,
            TestCoverageService testCoverageService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(projectStructureService, dependencyAnalysisService, testCoverageService)
                .build();
    }
}