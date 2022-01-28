package com.repsly.lunchsuggestionsystem.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmazonS3 {
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
