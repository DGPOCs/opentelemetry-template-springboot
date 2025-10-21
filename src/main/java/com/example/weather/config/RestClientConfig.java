package com.example.weather.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     ObjectProvider<ObservationRestTemplateCustomizer> observationCustomizer) {
        RestTemplateBuilder restTemplateBuilder = builder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        ObservationRestTemplateCustomizer customizer = observationCustomizer.getIfAvailable();
        if (customizer != null) {
            restTemplateBuilder = restTemplateBuilder.additionalCustomizers(customizer);
        }
        return restTemplateBuilder.build();
    }
}
