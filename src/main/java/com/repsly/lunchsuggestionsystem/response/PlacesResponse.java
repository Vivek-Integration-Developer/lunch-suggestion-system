package com.repsly.lunchsuggestionsystem.response;

import lombok.Data;

import java.util.List;

@Data
public class PlacesResponse {
    private List<Result> results;
    private String status;
}
