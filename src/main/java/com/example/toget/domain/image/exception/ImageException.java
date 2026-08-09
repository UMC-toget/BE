package com.example.toget.domain.image.exception;

import com.example.toget.domain.image.exception.code.ImageErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

/**
 * Image 도메인 예외.
 * ProjectException을 상속하므로 GeneralExceptionAdvice가 잡아서
 * 에러코드에 맞는 HTTP 상태 + ApiResponse JSON으로 변환해 준다.
 */
public class ImageException extends ProjectException {

    public ImageException(ImageErrorCode code) {
        super(code);
    }
}
