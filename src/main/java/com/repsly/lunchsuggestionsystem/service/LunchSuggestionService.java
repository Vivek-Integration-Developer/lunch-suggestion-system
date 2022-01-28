package com.repsly.lunchsuggestionsystem.service;

import org.springframework.http.ResponseEntity;

public interface LunchSuggestionService {

    /**
     * Upload the latest restaurant information details in form of CSV in S3 bucket
     * */
    ResponseEntity uploadNearbyRestaurants();

    /**
     * Download the nearby restaurants file from Amazon S3 bucket
     * */
    ResponseEntity downloadFileForNearbyRestaurants();
}
