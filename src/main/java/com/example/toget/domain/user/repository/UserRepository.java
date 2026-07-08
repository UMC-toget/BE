package com.example.toget.domain.user.repository;

import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * users 테이블 접근 리포지토리.
 * JpaRepository<엔티티, ID타입>을 상속하면 save/findById/delete 등 기본 CRUD가 자동 제공되고,
 * 구현체는 스프링 데이터가 런타임에 만들어 준다 (우리는 인터페이스만 정의).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // 쿼리 메서드: 메서드 이름을 파싱해 쿼리를 자동 생성
    // findBy + OAuthProvider + And + OAuthId
    // → SELECT * FROM users WHERE oauth_provider = ? AND oauth_id = ?
    // 반환형 Optional: 결과가 없을 수 있음을 타입으로 강제 (null 체크 누락 방지)
    Optional<User> findByOAuthProviderAndOAuthId(OAuthProvider oAuthProvider, String oAuthId);
}
