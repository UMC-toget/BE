package com.example.toget.domain.user.repository;

import com.example.toget.domain.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** user_accounts 테이블 접근 리포지토리 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // findAllBy + UserId + OrderBy + Id + Asc
    // → SELECT * FROM user_accounts WHERE user_id = ? ORDER BY user_account_id ASC
    // (등록한 순서대로 보여주기 위해 ID 오름차순 정렬)
    List<UserAccount> findAllByUserIdOrderByIdAsc(Long userId);

    // 회원 탈퇴 시 등록 계좌 전체 삭제 — 계좌번호도 개인정보이므로 익명화 정책과 함께 파기한다
    void deleteAllByUserId(Long userId);
}
