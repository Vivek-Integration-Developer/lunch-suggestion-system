package com.repsly.lunchsuggestionsystem.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenWeatherApi {
    private String apiKey;
    private String urlScheme;
    private String urlHost;
    private String path;
    private String exclude;
}
