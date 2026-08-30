package com.example.toget.domain.user.service;

/**
 * 가입 토큰(signup token)에 담기는 소셜 인증 결과.
 *
 * <p>소셜 인증은 끝났지만 아직 users 레코드가 없는 사람의 정보다. 프로필 설정 API가
 * 이 값으로 회원을 생성하므로, <b>클라이언트가 위조할 수 없도록 반드시 서명된 토큰 안에만</b> 존재해야 한다.
 * (요청 본문으로 provider·oAuthId를 그냥 받으면 남의 소셜 계정으로 가입할 수 있다)
 *
 * <p>provider를 enum이 아닌 String으로 두는 이유: JWT 클레임에서 읽어온 원본 문자열이라
 * 유효하지 않은 값일 수 있다. enum 변환(OAuthProvider.from)은 검증과 함께 서비스에서 수행한다.
 */
public record SignupClaims(
        String provider,
        String oAuthId,
        String email,
        String name,
        String profileImageUrl
) {
}
