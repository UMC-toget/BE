package com.example.toget.domain.image.controller;

import com.example.toget.domain.image.dto.PresignedUrlRequest;
import com.example.toget.domain.image.dto.PresignedUrlResponse;
import com.example.toget.domain.image.service.S3PresignedUrlService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이미지 API", description = "AWS S3 Presigned URL 발급 및 이미지 관련 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class S3Controller {

    private final S3PresignedUrlService s3PresignedUrlService;

    @Operation(summary = "S3 Presigned URL 발급", description = "이미지 Direct Upload를 위한 AWS S3 Presigned PUT URL 및 최종 이미지 URL을 발급받습니다.")
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        PresignedUrlResponse response = s3PresignedUrlService.generatePresignedUrl(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
