package com.repsly.lunchsuggestionsystem.utils;

import com.repsly.lunchsuggestionsystem.entity.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstantsUtil {

    public static final String BUSINESS_STATUS = "OPERATIONAL";
    private static List<Location> locationList = new ArrayList<>();

    static {
        locationList.add(Location.builder().name("55 Summer St, Boston, MA 02110, United States").latitude(42.35413753618642).longitude(-71.05880385980416).build());
        locationList.add(Location.builder().name("Ul. Franje Petračića 4, 10110, Zagreb, Croatia").latitude(45.808040089254575).longitude(15.957061015187563).build());
    }

    // Creating a list of Repsly office locations
    public static final List<Location> LOCATION_LIST = Collections.unmodifiableList(locationList);
}
