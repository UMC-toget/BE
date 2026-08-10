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
        Optional<BankName> bank = BankDetector.detect("3333011234567");
        assertThat(bank).isPresent().contains(BankName.KAKAO_BANK);
    }

    @Test
    @DisplayName("토스뱅크 1000 계좌 추론 테스트")
    void detectTossBank() {
        Optional<BankName> bank = BankDetector.detect("100012345678");
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
        Optional<BankName> bank = BankDetector.detect("1002123456789");
        assertThat(bank).isPresent().contains(BankName.WOORI);
    }

    @Test
    @DisplayName("신한은행 110 계좌 추론 테스트")
    void detectShinhanBank() {
        Optional<BankName> bank = BankDetector.detect("110123456789");
        assertThat(bank).isPresent().contains(BankName.SHINHAN);
    }

    @Test
    @DisplayName("KB국민은행 계좌 추론 테스트")
    void detectKBBank() {
        Optional<BankName> bank = BankDetector.detect("0123456789");
        assertThat(bank).isPresent().contains(BankName.KB);
    }

    @Test
    @DisplayName("NH농협은행 301 계좌 추론 테스트")
    void detectNHBank() {
        Optional<BankName> bank = BankDetector.detect("3011234567890");
        assertThat(bank).isPresent().contains(BankName.NH);
    }

    @Test
    @DisplayName("하나은행 611 차세대 계좌 추론 테스트")
    void detectHanaBank() {
        Optional<BankName> bank = BankDetector.detect("611123456789");
        assertThat(bank).isPresent().contains(BankName.HANA);
    }

    @Test
    @DisplayName("IBK기업은행 계좌 추론 테스트")
    void detectIBKBank() {
        Optional<BankName> bank = BankDetector.detect("001010123456");
        assertThat(bank).isPresent().contains(BankName.IBK);
    }

    @Test
    @DisplayName("새마을금고 계좌 추론 테스트")
    void detectMGSaemaeul() {
        Optional<BankName> bank = BankDetector.detect("9123456789012");
        assertThat(bank).isPresent().contains(BankName.MG_SAEMAEUL);
    }

    @Test
    @DisplayName("수협은행 계좌 추론 테스트")
    void detectSuhyupBank() {
        Optional<BankName> bank = BankDetector.detect("10001123456");
        assertThat(bank).isPresent().contains(BankName.SUHYUP);
    }

    @Test
    @DisplayName("iM뱅크(대구은행) 계좌 추론 테스트")
    void detectIMBank() {
        Optional<BankName> bank = BankDetector.detect("001050123456");
        assertThat(bank).isPresent().contains(BankName.IM_BANK);
    }

    @Test
    @DisplayName("부산은행 계좌 추론 테스트")
    void detectBusanBank() {
        Optional<BankName> bank = BankDetector.detect("1011234567890");
        assertThat(bank).isPresent().contains(BankName.BUSAN);
    }

    @Test
    @DisplayName("광주은행 계좌 추론 테스트")
    void detectGwangjuBank() {
        Optional<BankName> bank = BankDetector.detect("107123456789");
        assertThat(bank).isPresent().contains(BankName.GWANGJU);
    }

    @Test
    @DisplayName("경남은행 계좌 추론 테스트")
    void detectGyeongnamBank() {
        Optional<BankName> bank = BankDetector.detect("2071234567890");
        assertThat(bank).isPresent().contains(BankName.GYEONGNAM);
    }

    @Test
    @DisplayName("KDB산업은행 계좌 추론 테스트")
    void detectKDBBank() {
        Optional<BankName> bank = BankDetector.detect("01312345678");
        assertThat(bank).isPresent().contains(BankName.KDB);
    }

    @Test
    @DisplayName("SC제일은행 계좌 추론 테스트")
    void detectSCBank() {
        Optional<BankName> bank = BankDetector.detect("10012345678");
        assertThat(bank).isPresent().contains(BankName.SC);
    }

    @Test
    @DisplayName("우체국 530 계좌 추론 테스트")
    void detectPostOffice() {
        Optional<BankName> bank = BankDetector.detect("530123456789");
        assertThat(bank).isPresent().contains(BankName.POST_OFFICE);
    }

    @Test
    @DisplayName("한국씨티은행 계좌 추론 테스트")
    void detectCitiBank() {
        Optional<BankName> bank = BankDetector.detect("21012345678");
        assertThat(bank).isPresent().contains(BankName.CITI);
    }

    @Test
    @DisplayName("신협 계좌 추론 테스트")
    void detectShinhyup() {
        Optional<BankName> bank = BankDetector.detect("1311234567890");
        assertThat(bank).isPresent().contains(BankName.SHINHYUP);
    }

    @Test
    @DisplayName("전북은행 계좌 추론 테스트")
    void detectJeonbukBank() {
        Optional<BankName> bank = BankDetector.detect("5011234567890");
        assertThat(bank).isPresent().contains(BankName.JEONBUK);
    }

    @Test
    @DisplayName("제주은행 계좌 추론 테스트")
    void detectJejuBank() {
        Optional<BankName> bank = BankDetector.detect("010123456789");
        assertThat(bank).isPresent().contains(BankName.JEJU);
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
