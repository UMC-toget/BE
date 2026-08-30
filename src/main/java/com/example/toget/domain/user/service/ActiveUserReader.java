package com.example.toget.domain.user.service;

import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 활성(탈퇴하지 않은) 사용자 조회를 한 곳으로 모은 공용 검사기.
 *
 * [왜 필요한가] JWT는 발급 후 만료 전까지 계속 유효하다. 즉 탈퇴 직후에도
 * 기존 access token으로 API를 호출할 수 있으므로, 토큰 검증(필터)과 별개로
 * "지금도 활성 회원인가"를 DB에서 다시 확인해야 한다.
 * 여러 서비스에 같은 검사를 복붙하는 대신 한 클래스로 모아 규칙 변경 지점을 하나로 유지한다.
 */
@Component
@RequiredArgsConstructor
public class ActiveUserReader {

    private final UserRepository userRepository;

    /** 활성 사용자 조회 — 없으면 404(USER_NOT_FOUND), 탈퇴 상태면 401(UNAUTHORIZED) */
    public User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
