package org.example.riskintelligenceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BusinessWebClientConfig {

    @Bean
    public WebClient businessWebClient(
            WebClient.Builder builder,
            @Value("${services.business.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }
}