package com.example.toget.domain.gift.service;

import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.repository.FundingGiftVoteRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.gift.entity.FundingGiftVote;
import com.example.toget.domain.gift.repository.FundingGiftRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * toggleVote()의 "락 후 카운트"가 실제 MySQL(REPEATABLE READ) 트랜잭션 위에서도
 * 멤버당 최대 투표 수(3표) 제한을 지키는지 검증하는 동시성 통합 테스트 (issue #83).
 *
 * [왜 Mockito 단위 테스트로는 부족한가]
 *  - FundingGiftServiceTest는 findByFundingIdAndUserIdForUpdate가 "호출됐는지"만 검증할 뿐,
 *    실제 DB의 트랜잭션 스냅샷/락 대기 동작은 흉내내지 않는다.
 *  - 락 조회를 트랜잭션의 첫 statement로 옮겨도, 순서가 다시 흐트러지면(회귀) 단위 테스트는
 *    이를 잡아내지 못한다 — 오직 실제 DB에서 두 트랜잭션을 동시에 굴려봐야 검증 가능하다.
 *
 * [테스트 시나리오]
 *  이미 2표를 투표해 둔 멤버가, 아직 투표하지 않은 서로 다른 두 후보(A, B)에
 *  거의 동시에(CountDownLatch로 동기화) 투표를 시도한다.
 *  기대 동작: 둘 중 하나만 성공하고 나머지 하나는 VOTE_LIMIT_EXCEEDED로 거부되어,
 *  최종 투표 수는 어떤 인터리빙에서도 항상 3을 넘지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class FundingGiftVoteConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // 컨텍스트 로딩에 필요한 필수값(기본값 없음)만 테스트용 더미로 채운다.
        registry.add("jwt.secret", () -> "test-jwt-secret-not-for-production-use-only");
        registry.add("encryption.key", () -> "0123456789abcdef0123456789abcdef");
    }

    @Autowired
    private FundingGiftService fundingGiftService;

    @Autowired
    private FundingRepository fundingRepository;

    @Autowired
    private FundingMemberRepository fundingMemberRepository;

    @Autowired
    private FundingGiftRepository fundingGiftRepository;

    @Autowired
    private FundingGiftVoteRepository fundingGiftVoteRepository;

    @Test
    @DisplayName("이미 2표를 투표한 멤버가 서로 다른 두 후보에 동시에 투표해도 최종 투표 수는 3을 넘지 않는다")
    void concurrentVotes_neverExceedMaxVoteCount() throws InterruptedException {
        Long userId = 1L;
        Funding funding = fundingRepository.save(Funding.createTogetherGift(
                userId, null, "생일 파티", "김철수", LocalDate.now(),
                null, null, "함께 준비해요", null, 100_000L
        ));
        FundingMember member = fundingMemberRepository.save(
                FundingMember.createParticipant(funding.getId(), userId));

        // 기존에 이미 2표를 채워둔다 — 딱 1표만 더 여유 있는 상태로 만들어 레이스 조건을 노린다.
        FundingGift alreadyVoted1 = saveCandidate(funding.getId(), member.getId(), "이미 투표한 후보 1");
        FundingGift alreadyVoted2 = saveCandidate(funding.getId(), member.getId(), "이미 투표한 후보 2");
        fundingGiftVoteRepository.save(FundingGiftVote.create(alreadyVoted1.getId(), member.getId()));
        fundingGiftVoteRepository.save(FundingGiftVote.create(alreadyVoted2.getId(), member.getId()));

        FundingGift candidateA = saveCandidate(funding.getId(), member.getId(), "새 후보 A");
        FundingGift candidateB = saveCandidate(funding.getId(), member.getId(), "새 후보 B");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger limitExceededCount = new AtomicInteger();

        Runnable voteA = voteTask(userId, funding.getId(), candidateA.getId(),
                readyLatch, startLatch, doneLatch, successCount, limitExceededCount);
        Runnable voteB = voteTask(userId, funding.getId(), candidateB.getId(),
                readyLatch, startLatch, doneLatch, successCount, limitExceededCount);

        executor.submit(voteA);
        executor.submit(voteB);

        // 두 스레드가 각자 준비를 마칠 때까지 기다렸다가, 동시에 출발시킨다.
        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("두 투표 요청이 제한 시간 안에 끝나야 한다(데드락 의심)").isTrue();

        long totalVotes = fundingGiftVoteRepository.findAllByFundingMemberId(member.getId()).size();
        assertThat(totalVotes).isLessThanOrEqualTo(3);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(limitExceededCount.get()).isEqualTo(1);
    }

    private FundingGift saveCandidate(Long fundingId, Long registrantMemberId, String name) {
        return fundingGiftRepository.save(FundingGift.createCandidate(
                fundingId, registrantMemberId, name, 50_000L, null, null, null));
    }

    private Runnable voteTask(
            Long userId, Long fundingId, Long fundingGiftId,
            CountDownLatch readyLatch, CountDownLatch startLatch, CountDownLatch doneLatch,
            AtomicInteger successCount, AtomicInteger limitExceededCount
    ) {
        return () -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                fundingGiftService.toggleVote(userId, fundingId, fundingGiftId);
                successCount.incrementAndGet();
            } catch (com.example.toget.domain.gift.exception.FundingGiftException e) {
                if (e.getCode() == com.example.toget.domain.gift.exception.code.FundingGiftErrorCode.VOTE_LIMIT_EXCEEDED) {
                    limitExceededCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };
    }
}
