package com.repsly.lunchsuggestionsystem.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "application")
@NoArgsConstructor
@Data
@AllArgsConstructor
public class ApplicationConfiguration {
    private PlacesApi placesApi;
    private OpenWeatherApi openWeatherApi;
}
