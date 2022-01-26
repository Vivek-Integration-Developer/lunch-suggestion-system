package com.repsly.lunchsuggestionsystem.service.impl;

import com.google.gson.Gson;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.repsly.lunchsuggestionsystem.configuration.ApplicationConfiguration;
import com.repsly.lunchsuggestionsystem.entity.Location;
import com.repsly.lunchsuggestionsystem.entity.RestaurantDetails;
import com.repsly.lunchsuggestionsystem.response.*;
import com.repsly.lunchsuggestionsystem.service.LunchSuggestionService;
import com.repsly.lunchsuggestionsystem.utils.ConstantsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LunchSuggestionServiceImpl implements LunchSuggestionService {

    @Autowired
    ApplicationConfiguration applicationConfiguration;

    /**
     * Upload the latest restaurant information details in form of CSV in S3 bucket
     * */
    @Override
    public ResponseEntity uploadNearbyRestaurants() {
        try {
            List<RestaurantDetails> restaurantDetailsList = new ArrayList<>();
            for (Location location : ConstantsUtil.LOCATION_LIST) {
                //Places API call
                URIBuilder uriBuilder = new URIBuilder();
                uriBuilder.setScheme(applicationConfiguration.getPlacesApi().getUrlScheme());
                uriBuilder.setHost(applicationConfiguration.getPlacesApi().getUrlHost());
                uriBuilder.setPath(applicationConfiguration.getPlacesApi().getPath());
                uriBuilder.setParameters(createRequestParamsForPlacesAPI(location));
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uriBuilder.build())
                        .GET().build();
                log.info("Calling Google places API to get the restaurant details...");
                HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
                PlacesResponse placesResponse = new Gson().fromJson(response.body().toString(),PlacesResponse.class);
                log.info("Successfully fetched Google places API response");
                for(Result result: placesResponse.getResults()) {
                    if (result.getBusiness_status().equals(ConstantsUtil.BUSINESS_STATUS) &&
                            result.getOpening_hours().isOpened() && result.getRating() != null) {
                        //Open weather API call
                        uriBuilder.setScheme(applicationConfiguration.getOpenWeatherApi().getUrlScheme());
                        uriBuilder.setHost(applicationConfiguration.getOpenWeatherApi().getUrlHost());
                        uriBuilder.setPath(applicationConfiguration.getOpenWeatherApi().getPath());
                        uriBuilder.setParameters(createRequestParamsForWeatherAPI(result.getGeometry()));
                        HttpRequest openWeatherRequest = HttpRequest.newBuilder()
                                .uri(uriBuilder.build())
                                .GET().build();
                        log.info("Calling Open weather API to get the restaurant details...");
                        response = client.send(openWeatherRequest, HttpResponse.BodyHandlers.ofString());
                        OpenWeatherResponse openWeatherResponse = new Gson().fromJson(response.body().toString(),OpenWeatherResponse.class);
                        log.info("Successfully fetched Open Weather API response");
                        Weather weather = openWeatherResponse.getCurrent().getWeather().get(0);
                        restaurantDetailsList.add(RestaurantDetails.builder().address(result.getVicinity()).location(location.getName())
                                .name(result.getName()).priceLevel(result.getPrice_level() == null ? 0 : result.getPrice_level())
                                .userRatingsTotal(result.getUser_ratings_total())
                                .weatherDescription(weather.getMain().concat(" - ").concat(weather.getDescription()))
                                .rating(result.getRating()).build());
                        break;
                    }
                }
            }
            log.info("Converting the data into CSV...");
            CSVWriter(restaurantDetailsList);
            return ResponseEntity.created(URI.create("/upload")).build();
        } catch (IOException | URISyntaxException | InterruptedException e) {
            log.error("Error while getting the response from google places API: {}",e.getLocalizedMessage());
            return ResponseEntity.internalServerError().body("Error while getting the response from google places API");
        } catch (Exception ex) {
            log.error("Error while uploading the restaurant information: {}",ex.getLocalizedMessage());
            return ResponseEntity.internalServerError().body("Error while uploading the restaurant information");
        }
    }

    /**
     * Request parameter list set for Open Weather API
     * */
    private List<NameValuePair> createRequestParamsForWeatherAPI(Geometry geometry) {
        List<NameValuePair> requestParamsList = new ArrayList<>();
        requestParamsList.add(new BasicNameValuePair("lat", geometry.getLocation().getLat().toString()));
        requestParamsList.add(new BasicNameValuePair("lon", geometry.getLocation().getLng().toString()));
        requestParamsList.add(new BasicNameValuePair("exclude", applicationConfiguration.getOpenWeatherApi().getExclude()));
        requestParamsList.add(new BasicNameValuePair("appid", applicationConfiguration.getOpenWeatherApi().getApiKey()));
        return requestParamsList;
    }

    /**
     * Request parameter list set for Google Places API
     * */
    private List<NameValuePair> createRequestParamsForPlacesAPI(Location location) {
        List<NameValuePair> requestParamsList = new ArrayList<>();
        requestParamsList.add(new BasicNameValuePair("location", location.getLatitude() +"," + location.getLongitude()));
        requestParamsList.add(new BasicNameValuePair("radius", applicationConfiguration.getPlacesApi().getRadius()));
        requestParamsList.add(new BasicNameValuePair("type", applicationConfiguration.getPlacesApi().getType()));
        requestParamsList.add(new BasicNameValuePair("key", applicationConfiguration.getPlacesApi().getApiKey()));
        return requestParamsList;
    }

    /**
     * Converting the data into CSV file and saving it locally
     * */
    private void CSVWriter(List<RestaurantDetails> restaurantDetailsList) throws Exception {
        final String CSV_LOCATION = "LunchSuggestion.csv";
        try {
            FileWriter writer = new FileWriter(CSV_LOCATION);
            writer.append("Location, Restaurant name, Price level, Rating, Total user ratings, Weather Condition, Address \n");
            ColumnPositionMappingStrategy mappingStrategy= new ColumnPositionMappingStrategy();
            mappingStrategy.setType(RestaurantDetails.class);
            String[] columns = new String[] { "location", "name", "priceLevel", "rating", "userRatingsTotal", "weatherDescription", "address" };
            mappingStrategy.setColumnMapping(columns);
            StatefulBeanToCsvBuilder<RestaurantDetails> builder= new StatefulBeanToCsvBuilder(writer);
            StatefulBeanToCsv beanWriter = builder.withMappingStrategy(mappingStrategy).build();
            beanWriter.write(restaurantDetailsList);
            writer.close();
        }
        catch (Exception e) {
            log.error("Error while converting the data into CSV: {}",e.getLocalizedMessage());
            throw new Exception("Error while converting the data into CSV");
        }
    }

}
