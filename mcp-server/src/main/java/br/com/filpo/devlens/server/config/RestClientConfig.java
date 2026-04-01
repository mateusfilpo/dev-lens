package br.com.filpo.devlens.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient mavenCentralRestClient(RestClient.Builder builder) {
        return builder.baseUrl("https://search.maven.org").build();
    }

    @Bean
    public RestClient osvRestClient(RestClient.Builder builder) {
        return builder.baseUrl("https://api.osv.dev").build();
    }
}