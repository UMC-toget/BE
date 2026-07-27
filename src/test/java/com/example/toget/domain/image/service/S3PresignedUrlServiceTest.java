package com.example.toget.domain.image.service;

import com.example.toget.domain.image.dto.PresignedUrlRequest;
import com.example.toget.domain.image.dto.PresignedUrlResponse;
import com.example.toget.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class S3PresignedUrlServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private S3PresignedUrlService s3PresignedUrlService;

    @BeforeEach
    void setUp() {
        s3PresignedUrlService = new S3PresignedUrlService(s3Presigner);
        ReflectionTestUtils.setField(s3PresignedUrlService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(s3PresignedUrlService, "region", "ap-northeast-2");
    }

    @Test
    @DisplayName("유효한 이미지 요청으로 Presigned URL을 생성한다.")
    void generatePresignedUrl_Success() throws Exception {
        // given
        PresignedUrlRequest request = new PresignedUrlRequest("profiles", "avatar.png", "image/png");
        given(presignedPutObjectRequest.url()).willReturn(URI.create("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/dummy-presigned-url").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedPutObjectRequest);

        // when
        PresignedUrlResponse response = s3PresignedUrlService.generatePresignedUrl(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.presignedUrl()).contains("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/");
        assertThat(response.imageUrl()).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/");
        assertThat(response.imageUrl()).endsWith(".png");
    }

    @Test
    @DisplayName("이미지가 아닌 MIME 타입 요청 시 예외가 발생한다.")
    void generatePresignedUrl_InvalidContentType() {
        // given
        PresignedUrlRequest request = new PresignedUrlRequest("profiles", "script.sh", "application/x-sh");

        // when & then
        assertThatThrownBy(() -> s3PresignedUrlService.generatePresignedUrl(request))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    @DisplayName("허용되지 않은 파일 확장자 요청 시 예외가 발생한다.")
    void generatePresignedUrl_InvalidExtension() {
        // given
        PresignedUrlRequest request = new PresignedUrlRequest("profiles", "malicious.exe", "image/png");

        // when & then
        assertThatThrownBy(() -> s3PresignedUrlService.generatePresignedUrl(request))
                .isInstanceOf(ProjectException.class);
    }
}
