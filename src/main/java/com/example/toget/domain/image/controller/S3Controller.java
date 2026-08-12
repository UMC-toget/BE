package com.example.toget.domain.image.controller;

import com.example.toget.domain.image.dto.ImageImportRequest;
import com.example.toget.domain.image.dto.ImageImportResponse;
import com.example.toget.domain.image.dto.PresignedUrlRequest;
import com.example.toget.domain.image.dto.PresignedUrlResponse;
import com.example.toget.domain.image.service.ExternalImageImportService;
import com.example.toget.domain.image.service.S3PresignedUrlService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이미지 API", description = "AWS S3 Presigned URL 발급 및 이미지 관련 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class S3Controller {

    private final S3PresignedUrlService s3PresignedUrlService;
    private final ExternalImageImportService externalImageImportService;

    @Operation(summary = "S3 Presigned URL 발급", description = "이미지 Direct Upload를 위한 AWS S3 Presigned PUT URL 및 최종 이미지 URL을 발급받습니다.")
    @PostMapping("/upload-requests")
    public ApiResponse<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        PresignedUrlResponse response = s3PresignedUrlService.generatePresignedUrl(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    @Operation(
            summary = "이미지 가져오기 (외부 URL → S3 재업로드)",
            description = """
                    웹 사진 검색 결과를 선택했을 때 호출합니다. 서버가 원본 이미지를 내려받아 우리 S3에 저장하고,
                    저장된 S3 URL을 반환합니다. **프로필/위시/선물에는 이 응답의 imageUrl을 저장하세요.**

                    검색 결과의 원본 URL을 그대로 저장하면 원본 사이트가 이미지를 내리거나 핫링크를 차단할 때 깨지고,
                    http:// URL은 HTTPS 프론트에서 mixed content로 차단됩니다.

                    제약: http/https만 허용, 최대 10MB, jpeg·png·webp·gif·heic·heif만 지원 (SVG는 보안상 미지원)
                    """
    )
    @ResponseStatus(HttpStatus.CREATED) // 응답 본문 코드(COMMON201_1)와 실제 HTTP 상태를 201로 일치시킨다
    @PostMapping("/imports")
    public ApiResponse<ImageImportResponse> importImage(@Valid @RequestBody ImageImportRequest request) {
        ImageImportResponse response = externalImageImportService.importImage(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response);
    }
}
