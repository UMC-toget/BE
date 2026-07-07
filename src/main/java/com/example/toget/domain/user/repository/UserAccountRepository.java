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
}
