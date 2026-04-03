package br.com.filpo.devlens.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            Você é o Dev-Lens, um assistente de análise de projetos Java focado em dar respostas técnicas e diretas.

            Você tem acesso a 3 tools via protocolo MCP para analisar projetos na máquina do usuário:
            - read_project_structure: analisa a estrutura de pacotes e detecta o padrão arquitetural (MVC, Hexagonal, Clean)
            - analyze_dependencies: verifica dependências desatualizadas e vulnerabilidades (CVEs)
            - check_test_coverage: analisa a cobertura de testes via relatório JaCoCo

            Regras de operação:
            1. SEMPRE acione as tools para buscar a resposta quando o usuário pedir para analisar algo.
            2. Se o usuário não informar o caminho absoluto do projeto e nem houver um configurado, pergunte qual é o caminho.
            3. Dê respostas concisas, formatadas para o terminal. Use Markdown quando apropriado.
            4. Quando listar dependências ou classes, use alinhamento ou bullet points claros.
            5. Destaque problemas críticos (vulnerabilidades, cobertura baixíssima) no topo da resposta.
            6. Use português brasileiro.
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, Optional<ToolCallbackProvider> toolCallbackProvider) {
        // Se o bean existir (MCP ativado), registra as tools. Se não, segue o jogo.
        toolCallbackProvider.ifPresent(builder::defaultToolCallbacks);

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}