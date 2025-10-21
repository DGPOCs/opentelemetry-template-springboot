package com.example.weather.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    @JsonProperty("lat")
    private double latitude;

    @JsonProperty("lon")
    private double longitude;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("timezone_offset")
    private int timezoneOffset;

    @JsonProperty("current")
    private JsonNode current;

    @JsonProperty("minutely")
    private List<JsonNode> minutely;

    @JsonProperty("hourly")
    private List<JsonNode> hourly;

    @JsonProperty("daily")
    private List<JsonNode> daily;

    @JsonProperty("alerts")
    private List<JsonNode> alerts;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getTimezoneOffset() {
        return timezoneOffset;
    }

    public void setTimezoneOffset(int timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    public JsonNode getCurrent() {
        return current;
    }

    public void setCurrent(JsonNode current) {
        this.current = current;
    }

    public List<JsonNode> getMinutely() {
        return minutely;
    }

    public void setMinutely(List<JsonNode> minutely) {
        this.minutely = minutely;
    }

    public List<JsonNode> getHourly() {
        return hourly;
    }

    public void setHourly(List<JsonNode> hourly) {
        this.hourly = hourly;
    }

    public List<JsonNode> getDaily() {
        return daily;
    }

    public void setDaily(List<JsonNode> daily) {
        this.daily = daily;
    }

    public List<JsonNode> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<JsonNode> alerts) {
        this.alerts = alerts;
    }
}
