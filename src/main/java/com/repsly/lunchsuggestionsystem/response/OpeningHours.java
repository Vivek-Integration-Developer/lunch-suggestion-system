
package com.repsly.lunchsuggestionsystem.response;

import lombok.Data;

@Data
public class OpeningHours {
    private Boolean open_now;

    public Boolean isOpened() {
        return open_now;
    }
}
