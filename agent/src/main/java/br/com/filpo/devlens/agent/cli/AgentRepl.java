package br.com.filpo.devlens.agent.cli;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@ConditionalOnProperty(name = "dev-lens.repl.enabled", havingValue = "true", matchIfMissing = true)
public class AgentRepl implements CommandLineRunner {

    private final ChatClient chatClient;

    public AgentRepl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        printBanner();

        String envProject = System.getenv("DEV_LENS_PROJECT");
        if (envProject != null && !envProject.isBlank()) {
            System.out.println("  [Info] Projeto registrado via DEV_LENS_PROJECT: " + envProject);
            System.out.println();
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\ndev-lens> ");
            String input = scanner.nextLine().trim();

            if (input.isBlank())
                continue;

            if ("sair".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                System.out.println("Encerrando Dev-Lens. Até mais!");
                break;
            }

            try {
                System.out.println("Analisando...\n");

                // Se o usuário já setou o path via env var, concatenamos implicitamente no
                // contexto
                String promptContext = input;
                if (envProject != null && !envProject.isBlank() && !input.contains("/") && !input.contains("\\")) {
                    promptContext = input + " (O projeto alvo está em: " + envProject + ")";
                }

                String response = chatClient.prompt()
                        .user(promptContext)
                        .call()
                        .content();

                System.out.println(response);
            } catch (Exception e) {
                System.err.println("Erro ao processar: " + e.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("""

                ========================================
                        Dev-Lens v0.0.1
                  Analisador de Projetos Java via IA
                ========================================

                Comandos:
                  - Faca perguntas sobre projetos Java (ex: 'analise a estrutura')
                  - Informe o caminho absoluto do projeto na pergunta (ex: 'C:/meu-projeto')
                  - Digite "sair" para encerrar
                """);
    }
}