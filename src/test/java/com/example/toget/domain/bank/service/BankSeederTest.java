package com.example.toget.domain.bank.service;

import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.domain.bank.repository.BankRepository;
import com.example.toget.global.config.BankIconProperties;
import com.example.toget.global.enums.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * BankSeeder 단위 테스트.
 *
 * <p>시더는 배포·재기동마다 실행되므로 "여러 번 돌아도 안전한가"가 가장 중요한 성질이다.
 * 특히 관리자가 API로 바꾼 아이콘을 재기동 때 시드 기본값으로 되돌려 버리면
 * "무배포로 아이콘 교체"라는 기능 목적 자체가 깨지므로, 그 회귀를 여기서 막는다.
 */
@ExtendWith(MockitoExtension.class)
class BankSeederTest {

    @Mock
    private BankRepository bankRepository;

    private static final String BASE_URL =
            "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/bank-icons/v1";

    private BankSeeder seederWith(BankIconProperties properties) {
        return new BankSeeder(bankRepository, properties);
    }

    @Nested
    @DisplayName("최초 실행")
    class FirstRun {

        @Test
        @DisplayName("banks가 비어 있으면 BankName enum 전체를 INSERT한다")
        void seedsAllBankNames() {
            given(bankRepository.findAll()).willReturn(List.of());
            BankSeeder seeder = seederWith(new BankIconProperties(BASE_URL, "svg"));

            seeder.run(null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Bank>> captor = ArgumentCaptor.forClass(List.class);
            verify(bankRepository).saveAll(captor.capture());

            List<Bank> inserted = captor.getValue();
            assertThat(inserted).hasSize(BankName.values().length);
            assertThat(inserted).extracting(Bank::getCode)
                    .containsExactlyInAnyOrder(BankName.values());
        }

        @Test
        @DisplayName("base-url이 설정되면 '베이스URL/은행코드.확장자' 형태로 아이콘 URL을 채운다")
        void fillsIconUrlFromBaseUrl() {
            given(bankRepository.findAll()).willReturn(List.of());
            BankSeeder seeder = seederWith(new BankIconProperties(BASE_URL, "svg"));

            seeder.run(null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Bank>> captor = ArgumentCaptor.forClass(List.class);
            verify(bankRepository).saveAll(captor.capture());

            Bank kakao = captor.getValue().stream()
                    .filter(bank -> bank.getCode() == BankName.KAKAO_BANK)
                    .findFirst()
                    .orElseThrow();
            assertThat(kakao.getIconUrl()).isEqualTo(BASE_URL + "/KAKAO_BANK.svg");
            assertThat(kakao.getDisplayName()).isEqualTo(BankName.KAKAO_BANK.getDisplayName());
            assertThat(kakao.isActive()).isTrue();
        }

        @Test
        @DisplayName("base-url이 없으면 아이콘 URL을 null로 둔다 — 아이콘 업로드 전에도 배포 가능해야 하므로")
        void leavesIconUrlNullWhenBaseUrlMissing() {
            given(bankRepository.findAll()).willReturn(List.of());
            BankSeeder seeder = seederWith(new BankIconProperties(null, null));

            seeder.run(null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Bank>> captor = ArgumentCaptor.forClass(List.class);
            verify(bankRepository).saveAll(captor.capture());

            assertThat(captor.getValue()).allSatisfy(bank -> assertThat(bank.getIconUrl()).isNull());
        }
    }

    @Nested
    @DisplayName("재실행 (멱등성)")
    class Rerun {

        @Test
        @DisplayName("이미 전부 시드되어 있으면 INSERT를 하지 않는다")
        void doesNotInsertWhenAlreadySeeded() {
            given(bankRepository.findAll()).willReturn(allBanksSeeded(BASE_URL));
            BankSeeder seeder = seederWith(new BankIconProperties(BASE_URL, "svg"));

            seeder.run(null);

            verify(bankRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("아이콘이 비어 있던 기존 행은 백필한다 — 아이콘 업로드가 스키마 배포보다 늦은 경우")
        void backfillsMissingIconUrl() {
            List<Bank> seeded = allBanksSeeded(null); // 아이콘 없이 먼저 배포된 상태
            given(bankRepository.findAll()).willReturn(seeded);
            BankSeeder seeder = seederWith(new BankIconProperties(BASE_URL, "svg"));

            seeder.run(null);

            assertThat(seeded).allSatisfy(bank ->
                    assertThat(bank.getIconUrl()).isEqualTo(BASE_URL + "/" + bank.getCode().name() + ".svg"));
            verify(bankRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("관리자가 바꿔 둔 아이콘은 덮어쓰지 않는다 — 무배포 교체가 재기동으로 되돌아가면 안 되므로")
        void preservesAdminModifiedIconUrl() {
            String adminSetUrl = "https://cdn.toget.com/custom/KAKAO_BANK.png";
            List<Bank> seeded = allBanksSeeded(null);
            Bank kakao = seeded.stream()
                    .filter(bank -> bank.getCode() == BankName.KAKAO_BANK)
                    .findFirst()
                    .orElseThrow();
            kakao.fillIconUrl(adminSetUrl);
            given(bankRepository.findAll()).willReturn(seeded);
            BankSeeder seeder = seederWith(new BankIconProperties(BASE_URL, "svg"));

            seeder.run(null);

            assertThat(kakao.getIconUrl()).isEqualTo(adminSetUrl);
        }
    }

    /** BankName 전체가 이미 시드된 상태를 만든다 */
    private static List<Bank> allBanksSeeded(String baseUrl) {
        return Arrays.stream(BankName.values())
                .map(bankName -> Bank.builder()
                        .code(bankName)
                        .displayName(bankName.getDisplayName())
                        .iconUrl(baseUrl == null ? null : baseUrl + "/" + bankName.name() + ".svg")
                        .sortOrder(bankName.ordinal())
                        .active(true)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
