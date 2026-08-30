package com.example.toget.domain.user.repository;

import com.example.toget.domain.user.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** user_accounts 테이블 접근 리포지토리 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // findAllBy + UserId + OrderBy + Id + Asc
    // → SELECT * FROM user_accounts WHERE user_id = ? ORDER BY user_account_id ASC
    // (등록한 순서대로 보여주기 위해 ID 오름차순 정렬)
    //
    // @EntityGraph: 응답에 은행 아이콘·표시명이 들어가므로 bank를 반드시 함께 읽는다.
    // 없으면 UserAccount.bank가 LAZY라서 계좌 N개당 은행 SELECT가 N번 더 나간다(N+1).
    // 목록 조회라 계좌 수만큼 곱해지므로 특히 영향이 크다.
    @EntityGraph(attributePaths = "bank")
    List<UserAccount> findAllByUserIdOrderByIdAsc(Long userId);

    // 단건 조회 — 수정 응답에도 은행 아이콘이 나가므로 마찬가지로 bank를 함께 읽는다.
    // JpaRepository.findById에는 @EntityGraph를 붙일 수 없어 별도로 선언했다.
    @EntityGraph(attributePaths = "bank")
    Optional<UserAccount> findWithBankById(Long userAccountId);

    // 회원 탈퇴 시 등록 계좌 전체 삭제 — 계좌번호도 개인정보이므로 익명화 정책과 함께 파기한다
    void deleteAllByUserId(Long userId);
}
