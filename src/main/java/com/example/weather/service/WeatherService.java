package com.example.weather.service;

import com.example.weather.config.WeatherProperties;
import com.example.weather.web.dto.WeatherResponse;
import com.example.weather.web.exception.ExternalServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final RestTemplate restTemplate;
    private final WeatherProperties properties;
    private final ObjectMapper objectMapper;

    public WeatherService(RestTemplate restTemplate, WeatherProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public WeatherResponse getWeather(double latitude, double longitude, String units, String language) {
        validateApiKey();
        URI uri = buildOneCallUri(latitude, longitude, units, language);
        RequestEntity<Void> request = new RequestEntity<>(HttpMethod.GET, uri);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body == null) {
                throw new ExternalServiceException("La respuesta de OpenWeatherMap fue vacía");
            }
            return objectMapper.treeToValue(body, WeatherResponse.class);
        } catch (HttpStatusCodeException ex) {
            log.error("Error al invocar OpenWeatherMap: {}", ex.getResponseBodyAsString(), ex);
            throw new ExternalServiceException(
                    "Error al invocar OpenWeatherMap: " + ex.getStatusCode().value(),
                    ex
            );
        } catch (JsonProcessingException ex) {
            log.error("No fue posible interpretar la respuesta de OpenWeatherMap", ex);
            throw new ExternalServiceException("No fue posible interpretar la respuesta de OpenWeatherMap", ex);
        }
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new ExternalServiceException("No se configuró la API key de OpenWeatherMap. Defina la propiedad weather.api-key o la variable de entorno OPENWEATHER_API_KEY.");
        }
    }

    private URI buildOneCallUri(double latitude, double longitude, String units, String language) {
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("/onecall")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("appid", properties.getApiKey())
                .queryParam("units", StringUtils.hasText(units) ? units : properties.getUnits())
                .queryParam("lang", StringUtils.hasText(language) ? language : properties.getLanguage())
                .build(true)
                .toUri();
    }
}
