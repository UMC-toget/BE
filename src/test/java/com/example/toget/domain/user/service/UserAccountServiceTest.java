package com.example.toget.domain.user.service;

import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.domain.bank.exception.BankException;
import com.example.toget.domain.bank.repository.BankRepository;
import com.example.toget.domain.user.dto.UserAccountRequest;
import com.example.toget.domain.user.dto.UserAccountResponse;
import com.example.toget.domain.user.dto.UserAccountUpdateRequest;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.global.enums.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserAccountService 단위 테스트.
 *
 * <p>은행 아이콘 기능이 붙으면서 생긴 두 가지 계약을 회귀 방지 목적으로 고정한다.
 * <ul>
 *   <li>계좌에 은행 마스터(bank)가 연결되고 응답에 아이콘 URL이 실린다</li>
 *   <li>PATCH에서 bankName을 안 보내면 은행 조회 자체를 하지 않는다(부분 수정 의미론)</li>
 * </ul>
 * 리포지토리는 mock으로 대체하고 서비스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private BankRepository bankRepository;
    @Mock
    private ActiveUserReader activeUserReader;

    @InjectMocks
    private UserAccountService userAccountService;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final String ICON_URL =
            "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/bank-icons/v1/KAKAO_BANK.svg";

    private static Bank kakaoBank(String iconUrl) {
        return Bank.builder()
                .code(BankName.KAKAO_BANK)
                .displayName("카카오뱅크")
                .iconUrl(iconUrl)
                .sortOrder(BankName.KAKAO_BANK.ordinal())
                .active(true)
                .build();
    }

    private static UserAccount accountOf(Long userId, Bank bank) {
        return UserAccount.builder()
                .userId(userId)
                .bankName(BankName.KAKAO_BANK)
                .bank(bank)
                .accountOwner("홍길동")
                .account("3333011234567")
                .build();
    }

    @Nested
    @DisplayName("계좌 등록")
    class Create {

        @Test
        @DisplayName("요청한 은행 코드로 banks를 조회해 계좌에 연결한다")
        void linksBankOnCreate() {
            Bank bank = kakaoBank(ICON_URL);
            given(bankRepository.findByCode(BankName.KAKAO_BANK)).willReturn(Optional.of(bank));
            given(userAccountRepository.save(any(UserAccount.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            userAccountService.create(USER_ID,
                    new UserAccountRequest(BankName.KAKAO_BANK, "홍길동", "3333011234567"));

            ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
            verify(userAccountRepository).save(captor.capture());
            assertThat(captor.getValue().getBank()).isSameAs(bank);
            assertThat(captor.getValue().getBankName()).isEqualTo(BankName.KAKAO_BANK);
        }

        @Test
        @DisplayName("banks에 없는 은행이면 404 — 시더 미실행 같은 비정상 상태를 조용히 넘기지 않는다")
        void throwsWhenBankNotSeeded() {
            given(bankRepository.findByCode(BankName.KAKAO_BANK)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userAccountService.create(USER_ID,
                    new UserAccountRequest(BankName.KAKAO_BANK, "홍길동", "3333011234567")))
                    .isInstanceOf(BankException.class);

            verify(userAccountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("계좌 목록 조회")
    class GetMyAccounts {

        @Test
        @DisplayName("응답에 은행 아이콘 URL과 표시명이 실린다")
        void includesBankIconUrl() {
            given(userAccountRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                    .willReturn(List.of(accountOf(USER_ID, kakaoBank(ICON_URL))));

            List<UserAccountResponse> responses = userAccountService.getMyAccounts(USER_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).bankIconUrl()).isEqualTo(ICON_URL);
            assertThat(responses.get(0).bankDisplayName()).isEqualTo("카카오뱅크");
        }

        @Test
        @DisplayName("백필 전 레거시 계좌(bank=null)도 500이 아니라 아이콘 null로 응답한다")
        void handlesLegacyAccountWithoutBank() {
            given(userAccountRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                    .willReturn(List.of(accountOf(USER_ID, null)));

            List<UserAccountResponse> responses = userAccountService.getMyAccounts(USER_ID);

            assertThat(responses.get(0).bankIconUrl()).isNull();
            // 표시명은 enum 기본값으로 대체되어 화면이 비지 않는다
            assertThat(responses.get(0).bankDisplayName())
                    .isEqualTo(BankName.KAKAO_BANK.getDisplayName());
        }

        @Test
        @DisplayName("아이콘 미확보 은행이면 아이콘만 null이고 표시명은 정상 반환된다")
        void handlesBankWithoutIcon() {
            given(userAccountRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                    .willReturn(List.of(accountOf(USER_ID, kakaoBank(null))));

            List<UserAccountResponse> responses = userAccountService.getMyAccounts(USER_ID);

            assertThat(responses.get(0).bankIconUrl()).isNull();
            assertThat(responses.get(0).bankDisplayName()).isEqualTo("카카오뱅크");
        }
    }

    @Nested
    @DisplayName("계좌 수정 (PATCH 부분 수정)")
    class Update {

        @Test
        @DisplayName("bankName을 보내지 않으면 은행 조회 자체를 하지 않고 기존 은행을 유지한다")
        void keepsBankWhenBankNameOmitted() {
            Bank bank = kakaoBank(ICON_URL);
            given(userAccountRepository.findWithBankById(ACCOUNT_ID))
                    .willReturn(Optional.of(accountOf(USER_ID, bank)));

            UserAccountResponse response = userAccountService.update(USER_ID, ACCOUNT_ID,
                    new UserAccountUpdateRequest(null, "임꺽정", null));

            // 은행을 안 바꾸는 요청에 불필요한 조회가 나가면 PATCH 의미론이 깨진 것이다
            verify(bankRepository, never()).findByCode(any());
            assertThat(response.bankName()).isEqualTo(BankName.KAKAO_BANK);
            assertThat(response.bankIconUrl()).isEqualTo(ICON_URL);
            assertThat(response.accountOwner()).isEqualTo("임꺽정");
        }

        @Test
        @DisplayName("bankName을 바꾸면 은행 마스터도 함께 교체되어 아이콘이 새 은행 것으로 바뀐다")
        void swapsBankWhenBankNameChanged() {
            String kbIcon = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/bank-icons/v1/KB.svg";
            Bank kb = Bank.builder()
                    .code(BankName.KB)
                    .displayName("KB국민은행")
                    .iconUrl(kbIcon)
                    .sortOrder(BankName.KB.ordinal())
                    .active(true)
                    .build();
            given(userAccountRepository.findWithBankById(ACCOUNT_ID))
                    .willReturn(Optional.of(accountOf(USER_ID, kakaoBank(ICON_URL))));
            given(bankRepository.findByCode(BankName.KB)).willReturn(Optional.of(kb));

            UserAccountResponse response = userAccountService.update(USER_ID, ACCOUNT_ID,
                    new UserAccountUpdateRequest(BankName.KB, null, null));

            assertThat(response.bankName()).isEqualTo(BankName.KB);
            assertThat(response.bankIconUrl()).isEqualTo(kbIcon);
            assertThat(response.bankDisplayName()).isEqualTo("KB국민은행");
        }
    }
}
