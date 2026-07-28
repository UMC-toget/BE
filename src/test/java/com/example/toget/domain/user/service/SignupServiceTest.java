package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.SignupCompleteRequest;
import com.example.toget.domain.user.dto.SignupCompleteResponse;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.enums.UserStatus;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 회원가입 완료 처리 테스트 (issue #61).
 *
 * 핵심 계약 두 가지:
 *  1. 회원이 만들어지는 지점은 오직 여기다 — 닉네임과 함께 저장되고 곧바로 ACTIVE가 된다.
 *  2. 소셜 식별자는 서명된 가입 토큰에서만 온다 — 요청 본문으로 받으면 남의 계정으로 가입할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    private static final String SIGNUP_TOKEN = "signup-token";
    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenIssuer tokenIssuer;

    @InjectMocks
    private SignupService signupService;

    private final SignupClaims claims = new SignupClaims(
            "KAKAO", "kakao-1", "user@example.com", "홍길동", "https://toget.com/images/kakao.png");

    private SignupCompleteRequest request(String nickname, String profileImageUrl) {
        return new SignupCompleteRequest(SIGNUP_TOKEN, nickname, profileImageUrl);
    }

    /** saveAndFlush가 id를 채워 돌려주는 실제 JPA 동작을 흉내 낸다 */
    private void givenSaveAssignsId() {
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", USER_ID);
            return user;
        });
    }

    @Test
    @DisplayName("가입 토큰과 닉네임으로 회원이 생성되고 이 시점에 ACTIVE가 된다")
    void completeSignup_createsActiveUser() {
        given(jwtProvider.parseSignupToken(SIGNUP_TOKEN)).willReturn(claims);
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.empty());
        givenSaveAssignsId();
        given(tokenIssuer.issue(any(User.class))).willReturn(new TokenResponse("access", "refresh"));

        SignupCompleteResponse response =
                signupService.completeSignup(request("투겟러브", "https://toget.com/images/mine.png"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getNickname()).isEqualTo("투겟러브");
        assertThat(saved.getProfileImageUrl()).isEqualTo("https://toget.com/images/mine.png");
        // 소셜 식별자는 요청 본문이 아니라 토큰에서 왔는지 확인 — 위조 방지의 핵심
        assertThat(saved.getOAuthProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(saved.getOAuthId()).isEqualTo("kakao-1");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getName()).isEqualTo("홍길동");

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("프로필 이미지를 보내지 않으면 소셜에서 받아온 이미지를 기본값으로 쓴다")
    void completeSignup_fallsBackToSocialProfileImage() {
        given(jwtProvider.parseSignupToken(SIGNUP_TOKEN)).willReturn(claims);
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.empty());
        givenSaveAssignsId();
        given(tokenIssuer.issue(any(User.class))).willReturn(new TokenResponse("access", "refresh"));

        signupService.completeSignup(request("투겟러브", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getProfileImageUrl()).isEqualTo("https://toget.com/images/kakao.png");
    }

    @Test
    @DisplayName("이미 가입된 소셜 계정이면 ALREADY_REGISTERED(409)이고 저장하지 않는다")
    void completeSignup_alreadyRegistered_throwsConflict() {
        given(jwtProvider.parseSignupToken(SIGNUP_TOKEN)).willReturn(claims);
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.of(User.builder()
                        .oAuthProvider(OAuthProvider.KAKAO)
                        .oAuthId("kakao-1")
                        .nickname("기존닉")
                        .build()));

        assertThatThrownBy(() -> signupService.completeSignup(request("투겟러브", null)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getCode())
                .isEqualTo(UserErrorCode.ALREADY_REGISTERED);

        verify(userRepository, never()).saveAndFlush(any());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    @DisplayName("같은 가입 토큰이 동시에 두 번 들어와 유니크 제약에 걸려도 409로 변환된다")
    void completeSignup_concurrentDuplicate_throwsConflict() {
        given(jwtProvider.parseSignupToken(SIGNUP_TOKEN)).willReturn(claims);
        // 두 요청이 동시에 조회하면 둘 다 "없음"으로 통과할 수 있다 — 최종 방어선은 DB 유니크 제약
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.empty());
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> signupService.completeSignup(request("투겟러브", null)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getCode())
                .isEqualTo(UserErrorCode.ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("위조·만료된 가입 토큰이면 회원을 만들지 않고 401을 그대로 전파한다")
    void completeSignup_invalidSignupToken_throwsUnauthorized() {
        given(jwtProvider.parseSignupToken(SIGNUP_TOKEN))
                .willThrow(new UserException(UserErrorCode.UNAUTHORIZED));

        assertThatThrownBy(() -> signupService.completeSignup(request("투겟러브", null)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getCode())
                .isEqualTo(UserErrorCode.UNAUTHORIZED);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("토큰의 provider 값이 지원 목록에 없으면 400으로 거절한다")
    void completeSignup_unsupportedProvider_throwsBadRequest() {
        given(jwtProvider.parseSignupToken(SIGNUP_TOKEN)).willReturn(
                new SignupClaims("NAVER", "naver-1", null, null, null));

        assertThatThrownBy(() -> signupService.completeSignup(request("투겟러브", null)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getCode())
                .isEqualTo(UserErrorCode.UNSUPPORTED_PROVIDER);

        verify(userRepository, never()).saveAndFlush(any());
    }
}
