package com.example.toget.domain.image.service;

import com.example.toget.domain.image.dto.PresignedUrlRequest;
import com.example.toget.domain.image.dto.PresignedUrlResponse;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "svg"
    );

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * Presigned URL 및 최종 이미지 URL 생성
     */
    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        validateContentType(request.contentType());
        validateExtension(request.fileName());

        String key = createPath(request.prefix(), request.fileName());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedPutObjectRequest.url().toString();

        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);

        return new PresignedUrlResponse(presignedUrl, imageUrl);
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
        }
    }

    private void validateExtension(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        if (ext.isEmpty() || !ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
        }
    }

    private String createPath(String prefix, String fileName) {
        String ext = getFileExtension(fileName);
        String uuidFileName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        if (prefix != null && !prefix.trim().isEmpty()) {
            String cleanPrefix = prefix.replaceAll("^[./]+", "").replaceAll("/+", "/");
            return cleanPrefix + "/" + uuidFileName;
        }
        return uuidFileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
}
