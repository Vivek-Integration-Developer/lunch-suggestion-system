
package com.repsly.lunchsuggestionsystem.response;

import java.util.List;
import lombok.Data;

@Data
public class Current {
    private Double feelsLike;
    private Double temp;
    private List<Weather> weather;
}
