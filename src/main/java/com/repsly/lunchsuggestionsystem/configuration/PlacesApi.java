package com.repsly.lunchsuggestionsystem.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlacesApi {
    private String apiKey;
    private String urlScheme;
    private String urlHost;
    private String path;
    private String type;
    private String radius;
}
