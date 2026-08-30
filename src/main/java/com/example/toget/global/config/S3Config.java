package com.example.toget.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
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
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials()))
                .build();
    }

    /**
     * 서버가 직접 객체를 올릴 때 쓰는 클라이언트.
     * 프론트 Direct Upload(Presigned URL)와 달리, 웹 사진 검색 결과를 우리 S3로 옮겨오는
     * 이미지 가져오기(issue #102)는 서버가 바이트를 들고 있으므로 PutObject가 필요하다.
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials()))
                .build();
    }

    /**
     * 키가 없어도 빈 생성 자체는 성공해야 한다 — 로컬/테스트에서 S3 자격증명 없이 기동할 수 있어야 하므로.
     * 실제 호출 시점에 AWS가 403으로 거부한다.
     */
    private AwsBasicCredentials credentials() {
        String effectiveAccessKey = (accessKey != null && !accessKey.isBlank()) ? accessKey : "dummy-access-key";
        String effectiveSecretKey = (secretKey != null && !secretKey.isBlank()) ? secretKey : "dummy-secret-key";
        return AwsBasicCredentials.create(effectiveAccessKey, effectiveSecretKey);
    }
}
