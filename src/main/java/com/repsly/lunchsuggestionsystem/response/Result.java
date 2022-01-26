
package com.repsly.lunchsuggestionsystem.response;

import java.util.List;
import lombok.Data;

@Data
public class Result {

    private String business_status;
    private String name;
    private OpeningHours opening_hours;
    private Geometry geometry;
    private String place_id;
    private PlusCode plus_code;
    private Long price_level;
    private Double rating;
    private String reference;
    private String scope;
    private List<String> types;
    private Long user_ratings_total;
    private String vicinity;

}
