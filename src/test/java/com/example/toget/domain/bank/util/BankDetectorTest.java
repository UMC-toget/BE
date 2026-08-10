package com.example.toget.domain.bank.util;

import com.example.toget.global.enums.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BankDetectorTest {

    @Test
    @DisplayName("카카오뱅크 3333 계좌 추론 테스트")
    void detectKakaoBank() {
        Optional<BankName> bank = BankDetector.detect("3333-01-1234567");
        assertThat(bank).isPresent().contains(BankName.KAKAO_BANK);
    }

    @Test
    @DisplayName("토스뱅크 1000 계좌 추론 테스트")
    void detectTossBank() {
        Optional<BankName> bank = BankDetector.detect("1000-1234-5678");
        assertThat(bank).isPresent().contains(BankName.TOSS_BANK);
    }

    @Test
    @DisplayName("케이뱅크 9로 시작하는 10자리 계좌 추론 테스트")
    void detectKBank() {
        Optional<BankName> bank = BankDetector.detect("9012345678");
        assertThat(bank).isPresent().contains(BankName.K_BANK);
    }

    @Test
    @DisplayName("우리은행 1002 계좌 추론 테스트")
    void detectWooriBank() {
        Optional<BankName> bank = BankDetector.detect("1002-123-456789");
        assertThat(bank).isPresent().contains(BankName.WOORI);
    }

    @Test
    @DisplayName("신한은행 110 계좌 추론 테스트")
    void detectShinhanBank() {
        Optional<BankName> bank = BankDetector.detect("110-123-456789");
        assertThat(bank).isPresent().contains(BankName.SHINHAN);
    }

    @Test
    @DisplayName("하나은행 611 차세대 계좌 추론 테스트")
    void detectHanaBank() {
        Optional<BankName> bank = BankDetector.detect("611-123456-789");
        assertThat(bank).isPresent().contains(BankName.HANA);
    }

    @Test
    @DisplayName("NH농협 301 계좌 추론 테스트")
    void detectNHBank() {
        Optional<BankName> bank = BankDetector.detect("301-1234-5678-90");
        assertThat(bank).isPresent().contains(BankName.NH);
    }

    @Test
    @DisplayName("추론 불가능하거나 비정상적인 자릿수 입력 시 empty 반환")
    void detectInvalidAccount() {
        Optional<BankName> bankShort = BankDetector.detect("123");
        assertThat(bankShort).isEmpty();

        Optional<BankName> bankNull = BankDetector.detect(null);
        assertThat(bankNull).isEmpty();
    }
}
