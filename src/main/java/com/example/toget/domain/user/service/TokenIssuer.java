package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * access/refresh 토큰 발급 + Refresh Token Rotation 처리를 모은 공용 컴포넌트.
 *
 * <p>토큰이 발급되는 곳이 로그인(AuthService)과 가입 완료(SignupService) 두 군데로 늘어나면서,
 * "새 jti를 만들어 DB에 저장한다"는 rotation 규칙이 복사되지 않도록 한 곳으로 뺐다.
 * 한쪽만 저장을 빠뜨리면 그 경로로 받은 refresh token이 즉시 재사용 감지에 걸려 로그아웃된다.
 *
 * <p>[호출 규약] 반드시 트랜잭션 안에서 <b>영속 상태인</b> User로 호출해야 한다.
 * 엔티티 필드만 바꾸고 save()를 부르지 않으므로, 준영속 객체로 호출하면 jti가 DB에 반영되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtProvider jwtProvider;

    public TokenResponse issue(User user) {
        String tokenId = UUID.randomUUID().toString();
        // 트랜잭션 안에서 엔티티 필드만 바꾸면 커밋 시점에 JPA가 변경을 감지(dirty checking)해
        // UPDATE 쿼리를 자동 실행한다 — save()를 다시 부를 필요가 없다
        user.updateRefreshToken(tokenId);
        return new TokenResponse(
                jwtProvider.createAccessToken(user.getId()),
                jwtProvider.createRefreshToken(user.getId(), tokenId)
        );
    }
}
