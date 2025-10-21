package com.example.weather.service;

import com.example.weather.config.WeatherProperties;
import com.example.weather.web.dto.WeatherResponse;
import com.example.weather.web.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherServiceTest {

    private WeatherService weatherService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        WeatherProperties properties = new WeatherProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openweathermap.org/data/3.0");
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        server = MockRestServiceServer.createServer(restTemplate);
        weatherService = new WeatherService(restTemplate, properties, new ObjectMapper());
    }

    @Test
    void shouldRetrieveWeatherInformation() {
        String response = "{" +
                "\"lat\":10.0," +
                "\"lon\":20.0," +
                "\"timezone\":\"UTC\"," +
                "\"timezone_offset\":0," +
                "\"current\":{\"temp\":30}," +
                "\"minutely\":[{\"precipitation\":0}]," +
                "\"hourly\":[{\"temp\":31}]," +
                "\"daily\":[{\"temp\":{\"day\":32}}]," +
                "\"alerts\":[{\"event\":\"Storm\"}]" +
                "}";

        server.expect(requestTo("https://api.openweathermap.org/data/3.0/onecall?lat=10.0&lon=20.0&appid=test-key&units=metric&lang=es"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        WeatherResponse weatherResponse = weatherService.getWeather(10.0, 20.0, null, null);

        assertThat(weatherResponse.getLatitude()).isEqualTo(10.0);
        assertThat(weatherResponse.getLongitude()).isEqualTo(20.0);
        assertThat(weatherResponse.getCurrent().get("temp").asInt()).isEqualTo(30);
        assertThat(weatherResponse.getMinutely()).hasSize(1);
        assertThat(weatherResponse.getHourly()).hasSize(1);
        assertThat(weatherResponse.getDaily()).hasSize(1);
        assertThat(weatherResponse.getAlerts()).hasSize(1);
        server.verify();
    }

    @Test
    void shouldFailWhenApiKeyIsMissing() {
        WeatherProperties properties = new WeatherProperties();
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        WeatherService serviceWithoutKey = new WeatherService(restTemplate, properties, new ObjectMapper());

        assertThatThrownBy(() -> serviceWithoutKey.getWeather(0, 0, null, null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("No se configuró la API key de OpenWeatherMap");
    }
}
