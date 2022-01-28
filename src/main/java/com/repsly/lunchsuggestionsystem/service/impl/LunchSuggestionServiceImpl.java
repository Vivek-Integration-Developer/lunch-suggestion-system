package com.repsly.lunchsuggestionsystem.service.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
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
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
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
        File file = null;
        try {
            List<RestaurantDetails> restaurantDetailsList = new ArrayList<>();
            file = new File(ConstantsUtil.CSV_FILE_NAME);
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
            log.info("Uploading the file into S3 bucket");
            uploadFileInS3(file);
            return ResponseEntity.created(URI.create("/upload")).build();
        } catch (IOException | URISyntaxException | InterruptedException e) {
            log.error("Error while getting the response from google places API: {}",e.getLocalizedMessage());
            return ResponseEntity.internalServerError().body("Error while getting the response from google places API");
        } catch (Exception ex) {
            log.error("Error while uploading the restaurant information: {}",ex.getLocalizedMessage());
            return ResponseEntity.internalServerError().body("Error while uploading the restaurant information");
        } finally {
            try {
                if (file != null) {
                    log.info("Deleting the csv file from local");
                    FileUtils.forceDelete(file);
                }
            } catch (IOException e) {
                log.error("Error while deleting the file in local");
            }
        }
    }

    /**
     * Download the nearby restaurants file from Amazon S3 bucket
     * */
    @Override
    public ResponseEntity downloadFileForNearbyRestaurants() {
        File file = null;
        try {
            log.info("Started downloading the file for nearby restaurants");
            file= new File(ConstantsUtil.CSV_FILE_NAME);
            AmazonS3 s3client = getS3Client();
            S3Object s3object = s3client.getObject(applicationConfiguration.getS3().getBucketName(), ConstantsUtil.CSV_FILE_NAME);
            S3ObjectInputStream inputStream = s3object.getObjectContent();
            FileUtils.copyInputStreamToFile(inputStream, file);
            log.info("File has been downloaded successfully");
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + ConstantsUtil.CSV_FILE_NAME)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            log.error("Error while converting/downloading the file : {}", e.getLocalizedMessage());
            return ResponseEntity.internalServerError().body("Error while downloading the restaurant information");
        }
    }

    /**
     * Uploading the csv file in Amazon S3 bucket
     * */
    private void uploadFileInS3(File file) {
        AmazonS3 s3client = getS3Client();
        s3client.putObject(applicationConfiguration.getS3().getBucketName(), ConstantsUtil.CSV_FILE_NAME, file);
        log.info("File uploaded successfully in S3 bucket...");
    }

    /**
     * Get Amazon S3 client information
     * */
    private AmazonS3 getS3Client() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(applicationConfiguration.getS3().getAccessKey(),
                applicationConfiguration.getS3().getSecretKey());
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.US_EAST_1)
                .build();
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
        try {
            FileWriter writer = new FileWriter(ConstantsUtil.CSV_FILE_NAME);
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
