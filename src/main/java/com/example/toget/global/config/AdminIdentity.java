package com.example.toget.global.config;

import com.example.toget.domain.user.enums.OAuthProvider;

/**
 * 관리자 계정 1명을 식별하는 (이메일, 로그인 공급자) 쌍.
 *
 * <p>이메일만으로 비교하지 않는 이유는 {@link AdminProperties}를 참고.
 * 같은 이메일이라도 공급자가 다르면(GOOGLE/KAKAO) 별개의 항목으로 등록해야 한다 —
 * 한 사람이 이메일은 같지만 구글/카카오 두 계정 모두로 로그인해야 하는 경우가 그 예시다.
 */
public record AdminIdentity(String email, OAuthProvider provider) {

    public boolean isValid() {
        return email != null && !email.isBlank() && provider != null;
    }
}
