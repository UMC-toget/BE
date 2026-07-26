package com.example.toget.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Bean
    public S3Presigner s3Presigner() {
        String effectiveAccessKey = (accessKey != null && !accessKey.isBlank()) ? accessKey : "dummy-access-key";
        String effectiveSecretKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : "dummy-secret-key";
        AwsBasicCredentials credentials = AwsBasicCredentials.create(effectiveAccessKey, effectiveSecretKey);

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
