package com.repsly.lunchsuggestionsystem.controller;


import com.repsly.lunchsuggestionsystem.service.LunchSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/lunch-suggestion")
public class LunchSuggestionController {

    @Autowired
    LunchSuggestionService lunchSuggestionService;

    @PostMapping(path = "/upload")
    public ResponseEntity uploadNearbyRestaurants() {
        return lunchSuggestionService.uploadNearbyRestaurants();
    }

    @GetMapping(path = "/download", produces = "text/csv")
    public  ResponseEntity downloadNearbyRestaurants() {
        return lunchSuggestionService.downloadFileForNearbyRestaurants();
    }
}
