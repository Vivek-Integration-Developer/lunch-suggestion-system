
package com.repsly.lunchsuggestionsystem.response;

import lombok.Data;

@Data
public class OpenWeatherResponse {
    private Current current;
    private Double lat;
    private Double lon;
    private String timezone;
    private Long timezoneOffset;
}