package com.example.toget.domain.bank.service;

import com.example.toget.domain.bank.dto.BankDetectionRequest;
import com.example.toget.domain.bank.dto.BankDetectionResponse;
import com.example.toget.domain.bank.dto.BankResponse;
import com.example.toget.domain.bank.dto.BankUpdateRequest;
import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.domain.bank.exception.BankException;
import com.example.toget.domain.bank.repository.BankRepository;
import com.example.toget.global.enums.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * BankService 단위 테스트.
 * 관리자 수정이 부분 수정(PATCH) 의미론을 지키는지가 핵심 — 아이콘만 바꾸려다
 * 표시명이나 노출 순서가 초기화되면 운영 사고가 된다.
 */
@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private BankRepository bankRepository;

    @InjectMocks
    private BankService bankService;

    private static Bank kakaoBank() {
        return Bank.builder()
                .code(BankName.KAKAO_BANK)
                .displayName("카카오뱅크")
                .iconUrl("https://cdn.toget.com/bank-icons/v1/KAKAO_BANK.svg")
                .sortOrder(8)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("활성 은행 목록 조회")
    class GetActiveBanks {

        @Test
        @DisplayName("노출 순서대로 조회한 결과를 응답 DTO로 변환한다")
        void returnsActiveBanks() {
            given(bankRepository.findAllByActiveTrueOrderBySortOrderAsc())
                    .willReturn(List.of(kakaoBank()));

            List<BankResponse> responses = bankService.getActiveBanks();

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).code()).isEqualTo(BankName.KAKAO_BANK);
            assertThat(responses.get(0).displayName()).isEqualTo("카카오뱅크");
            assertThat(responses.get(0).iconUrl()).isEqualTo("https://cdn.toget.com/bank-icons/v1/KAKAO_BANK.svg");
        }
    }

    @Nested
    @DisplayName("계좌번호 기반 은행 추론")
    class DetectBank {

        @Test
        @DisplayName("계좌번호 입력 시 추론된 은행들의 DTO 목록을 반환한다")
        void returnsDetectedBankResponses() {
            given(bankRepository.findAllByCodeIn(List.of(BankName.KAKAO_BANK)))
                    .willReturn(List.of(kakaoBank()));

            List<BankDetectionResponse> responses = bankService.detectBank(new BankDetectionRequest("3333011234567"));

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).bankName()).isEqualTo(BankName.KAKAO_BANK);
            assertThat(responses.get(0).displayName()).isEqualTo("카카오뱅크");
        }
    }

    @Nested
    @DisplayName("은행 정보 수정")
    class Update {

        @Test
        @DisplayName("아이콘만 보내면 나머지 필드는 기존 값을 유지한다")
        void updatesOnlyProvidedFields() {
            Bank bank = kakaoBank();
            given(bankRepository.findById(1L)).willReturn(Optional.of(bank));
            String newIcon = "https://cdn.toget.com/bank-icons/v2/KAKAO_BANK.svg";

            BankResponse response = bankService.update(1L,
                    new BankUpdateRequest(null, newIcon, null, null));

            assertThat(response.iconUrl()).isEqualTo(newIcon);
            assertThat(response.displayName()).isEqualTo("카카오뱅크"); // 초기화되지 않아야 한다
            assertThat(bank.getSortOrder()).isEqualTo(8);
            assertThat(bank.isActive()).isTrue();
        }

        @Test
        @DisplayName("isActive=false로 내리면 목록에서 빠질 수 있도록 상태가 반영된다")
        void deactivatesBank() {
            Bank bank = kakaoBank();
            given(bankRepository.findById(1L)).willReturn(Optional.of(bank));

            bankService.update(1L, new BankUpdateRequest(null, null, null, false));

            assertThat(bank.isActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 은행이면 404")
        void throwsWhenBankNotFound() {
            given(bankRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bankService.update(999L,
                    new BankUpdateRequest(null, "https://cdn.toget.com/x.svg", null, null)))
                    .isInstanceOf(BankException.class);
        }
    }
}
