package com.repsly.lunchsuggestionsystem;

import com.repsly.lunchsuggestionsystem.configuration.ApplicationConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties(value = ApplicationConfiguration.class)
public class LunchSuggestionSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(LunchSuggestionSystemApplication.class, args);
	}

}
