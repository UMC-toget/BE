package com.example.toget.domain.user.service;

import com.example.toget.domain.user.converter.UserConverter;
import com.example.toget.domain.user.dto.UserProfileResponse;
import com.example.toget.domain.user.dto.UserProfileUpdateRequest;
import com.example.toget.domain.user.dto.UserProfileUpdateResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 프로필 조회/수정/탈퇴 비즈니스 로직.
 * 컨트롤러는 HTTP 처리만, 서비스는 규칙(활성 사용자인지, 무엇을 바꿀 수 있는지)을 담당하는 계층 분리 구조.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final ActiveUserReader activeUserReader;
    private final UserAccountRepository userAccountRepository;

    // readOnly = true: 조회 전용 트랜잭션 — JPA가 변경 감지(dirty checking) 준비를 생략해 더 가볍다
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        // 엔티티를 그대로 반환하지 않고 DTO로 변환 — refreshToken 같은 내부 필드 노출을 막는다
        return UserConverter.toProfileResponse(activeUserReader.getActiveUser(userId));
    }

    @Transactional
    public UserProfileUpdateResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = activeUserReader.getActiveUser(userId);
        // 엔티티 필드만 바꾸면 트랜잭션 커밋 시 JPA가 UPDATE를 자동 실행 (save 호출 불필요)
        user.updateProfile(request.nickname(), request.profileImageUrl());
        return UserConverter.toProfileUpdateResponse(user);
    }

    /** 프로필 이미지 초기화 (기본 이미지로 변경) */
    @Transactional
    public UserProfileUpdateResponse clearMyProfileImage(Long userId) {
        User user = activeUserReader.getActiveUser(userId);
        user.clearProfileImage();
        return UserConverter.toProfileUpdateResponse(user);
    }

    /**
     * 회원 탈퇴 — Soft Delete(status=WITHDRAWN) + 개인정보 익명화 + 세션(refresh_token) 만료.
     * 레코드를 물리 삭제하지 않는 이유: 정산 이력 등 연관 데이터의 FK 보존.
     * 대신 소셜 식별자(oauth_id)·이메일 등은 파기하므로 같은 소셜 계정으로 재가입할 수 있고(User.withdraw 참고),
     * 등록 계좌는 계좌번호가 개인정보라 함께 삭제한다.
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = activeUserReader.getActiveUser(userId);
        userAccountRepository.deleteAllByUserId(userId);
        user.withdraw();
    }
}
