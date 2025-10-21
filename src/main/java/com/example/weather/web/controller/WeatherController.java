package com.example.weather.web.controller;

import com.example.weather.service.WeatherService;
import com.example.weather.web.dto.WeatherResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@Validated
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherResponse getWeather(@RequestParam("lat") @NotNull Double latitude,
                                      @RequestParam("lon") @NotNull Double longitude,
                                      @RequestParam(value = "units", required = false) String units,
                                      @RequestParam(value = "lang", required = false) String language) {
        return weatherService.getWeather(latitude, longitude, units, language);
    }
}
