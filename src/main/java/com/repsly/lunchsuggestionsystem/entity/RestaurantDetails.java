package com.repsly.lunchsuggestionsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDetails {

    private String location;
    private String name;
    private Long priceLevel;
    private Double rating;
    private Long userRatingsTotal;
    private String address;
    private String weatherDescription;

}
