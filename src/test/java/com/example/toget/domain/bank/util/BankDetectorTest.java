package com.example.toget.domain.bank.util;

import com.example.toget.global.enums.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BankDetectorTest {

    public static void main(String[] args) {
        BankDetectorTest test = new BankDetectorTest();
        test.detectKakaoBank();
        test.detectTossBank();
        test.detectKBank();
        test.detectWooriBank();
        test.detectShinhanBank();
        test.detectKBBank();
        test.detectNHBank();
        test.detectHanaBank();
        test.detectIBKBank();
        test.detectMGSaemaeul();
        test.detectSuhyupBank();
        test.detectIMBank();
        test.detectBusanBank();
        test.detectGwangjuBank();
        test.detectGyeongnamBank();
        test.detectKDBBank();
        test.detectSCBank();
        test.detectPostOffice();
        test.detectCitiBank();
        test.detectShinhyup();
        test.detectJeonbukBank();
        test.detectJejuBank();
        test.detectAllBankEnumValues();
        test.detectInvalidAccount();
        test.detectPrecisionTest();
        System.out.println("✅ SUCCESS: All 22 Bank Detection tests passed cleanly!");
    }

    @Test
    @DisplayName("카카오뱅크 3333 계좌 추론 테스트")
    void detectKakaoBank() {
        List<BankName> banks = BankDetector.detectAll("3333011234567");
        assertThat(banks).contains(BankName.KAKAO_BANK);
    }

    @Test
    @DisplayName("토스뱅크 1000 계좌 추론 테스트")
    void detectTossBank() {
        List<BankName> banks = BankDetector.detectAll("100012345678");
        assertThat(banks).contains(BankName.TOSS_BANK);
    }

    @Test
    @DisplayName("케이뱅크 9로 시작하는 10자리 계좌 추론 테스트")
    void detectKBank() {
        List<BankName> banks = BankDetector.detectAll("9012345678");
        assertThat(banks).contains(BankName.K_BANK);
    }

    @Test
    @DisplayName("우리은행 1002 계좌 추론 테스트")
    void detectWooriBank() {
        List<BankName> banks = BankDetector.detectAll("1002123456789");
        assertThat(banks).contains(BankName.WOORI);
    }

    @Test
    @DisplayName("신한은행 110 계좌 추론 테스트")
    void detectShinhanBank() {
        List<BankName> banks = BankDetector.detectAll("110123456789");
        assertThat(banks).contains(BankName.SHINHAN);
    }

    @Test
    @DisplayName("KB국민은행 계좌 추론 테스트")
    void detectKBBank() {
        List<BankName> banks = BankDetector.detectAll("0123456789");
        assertThat(banks).contains(BankName.KB);
    }

    @Test
    @DisplayName("NH농협은행 301 계좌 추론 테스트")
    void detectNHBank() {
        List<BankName> banks = BankDetector.detectAll("3011234567890");
        assertThat(banks).contains(BankName.NH);
    }

    @Test
    @DisplayName("하나은행 611 차세대 계좌 추론 테스트")
    void detectHanaBank() {
        List<BankName> banks = BankDetector.detectAll("611123456789");
        assertThat(banks).contains(BankName.HANA);
    }

    @Test
    @DisplayName("IBK기업은행 계좌 추론 테스트")
    void detectIBKBank() {
        List<BankName> banks = BankDetector.detectAll("001010123456");
        assertThat(banks).contains(BankName.IBK);
    }

    @Test
    @DisplayName("새마을금고 계좌 추론 테스트")
    void detectMGSaemaeul() {
        List<BankName> banks = BankDetector.detectAll("9090123456789");
        assertThat(banks).contains(BankName.MG_SAEMAEUL);
    }

    @Test
    @DisplayName("수협은행 계좌 추론 테스트")
    void detectSuhyupBank() {
        List<BankName> banks = BankDetector.detectAll("10001123456");
        assertThat(banks).contains(BankName.SUHYUP);
    }

    @Test
    @DisplayName("iM뱅크(대구은행) 계좌 추론 테스트")
    void detectIMBank() {
        List<BankName> banks = BankDetector.detectAll("001050123456");
        assertThat(banks).contains(BankName.IM_BANK);
    }

    @Test
    @DisplayName("부산은행 계좌 추론 테스트")
    void detectBusanBank() {
        List<BankName> banks = BankDetector.detectAll("101107123456");
        assertThat(banks).contains(BankName.BUSAN);
    }

    @Test
    @DisplayName("광주은행 계좌 추론 테스트")
    void detectGwangjuBank() {
        List<BankName> banks = BankDetector.detectAll("107011234567");
        assertThat(banks).contains(BankName.GWANGJU);
    }

    @Test
    @DisplayName("경남은행 계좌 추론 테스트")
    void detectGyeongnamBank() {
        List<BankName> banks = BankDetector.detectAll("2070112345678");
        assertThat(banks).contains(BankName.GYEONGNAM);
    }

    @Test
    @DisplayName("KDB산업은행 계좌 추론 테스트")
    void detectKDBBank() {
        List<BankName> banks = BankDetector.detectAll("01311123456");
        assertThat(banks).contains(BankName.KDB);
    }

    @Test
    @DisplayName("SC제일은행 계좌 추론 테스트")
    void detectSCBank() {
        List<BankName> banks = BankDetector.detectAll("10010345678");
        assertThat(banks).contains(BankName.SC);
    }

    @Test
    @DisplayName("우체국 530 계좌 추론 테스트")
    void detectPostOffice() {
        List<BankName> banks = BankDetector.detectAll("530101234567");
        assertThat(banks).contains(BankName.POST_OFFICE);
    }

    @Test
    @DisplayName("한국씨티은행 계좌 추론 테스트")
    void detectCitiBank() {
        List<BankName> banks = BankDetector.detectAll("21001345678");
        assertThat(banks).contains(BankName.CITI);
    }

    @Test
    @DisplayName("신협 계좌 추론 테스트")
    void detectShinhyup() {
        List<BankName> banks = BankDetector.detectAll("1311311234567");
        assertThat(banks).contains(BankName.SHINHYUP);
    }

    @Test
    @DisplayName("전북은행 계좌 추론 테스트")
    void detectJeonbukBank() {
        List<BankName> banks = BankDetector.detectAll("5010112345678");
        assertThat(banks).contains(BankName.JEONBUK);
    }

    @Test
    @DisplayName("제주은행 계좌 추론 테스트")
    void detectJejuBank() {
        List<BankName> banks = BankDetector.detectAll("010011234567");
        assertThat(banks).contains(BankName.JEJU);
    }

    @Test
    @DisplayName("BankName enum에 존재하는 모든 22개 은행이 최소 1개 이상의 샘플 계좌번호로 추론 가능하다")
    void detectAllBankEnumValues() {
        for (BankName bankName : BankName.values()) {
            String sampleAccount = switch (bankName) {
                case KB -> "0123456789";
                case SHINHAN -> "110123456789";
                case WOORI -> "1002123456789";
                case HANA -> "611123456789";
                case NH -> "3011234567890";
                case IBK -> "001010123456";
                case SC -> "10010345678";
                case CITI -> "21001345678";
                case KAKAO_BANK -> "3333011234567";
                case TOSS_BANK -> "100012345678";
                case K_BANK -> "9012345678";
                case POST_OFFICE -> "530101234567";
                case MG_SAEMAEUL -> "9090123456789";
                case SHINHYUP -> "1311311234567";
                case SUHYUP -> "10001123456";
                case BUSAN -> "101107123456";
                case IM_BANK -> "001050123456";
                case GWANGJU -> "107011234567";
                case JEONBUK -> "5010112345678";
                case GYEONGNAM -> "2070112345678";
                case JEJU -> "010011234567";
                case KDB -> "01311123456";
            };

            List<BankName> detected = BankDetector.detectAll(sampleAccount);
            assertThat(detected)
                    .as("%s (%s) 은행이 계좌번호 %s 로 추론되어야 합니다.", bankName.name(), bankName.getDisplayName(), sampleAccount)
                    .contains(bankName);
        }
    }

    @Test
    @DisplayName("추론 불가능하거나 비정상적인 자릿수 입력 시 빈 리스트 반환")
    void detectInvalidAccount() {
        List<BankName> bankShort = BankDetector.detectAll("123");
        assertThat(bankShort).isEmpty();

        List<BankName> bankNull = BankDetector.detectAll(null);
        assertThat(bankNull).isEmpty();
    }

    @Test
    @DisplayName("고신뢰 프리픽스 일치 계좌번호 입력 시 정답 은행이 1순위(최상위)로 정확하게 추론된다")
    void detectPrecisionTest() {
        // 110으로 시작하는 신한 12자리 계좌번호 -> 최상위 정답 신한은행 검증
        List<BankName> shinhanResult = BankDetector.detectAll("110123456789");
        assertThat(shinhanResult.get(0)).isEqualTo(BankName.SHINHAN);

        // 3333 카카오뱅크 13자리 계좌번호 -> 최상위 정답 카카오뱅크 검증
        List<BankName> kakaoResult = BankDetector.detectAll("3333011234567");
        assertThat(kakaoResult.get(0)).isEqualTo(BankName.KAKAO_BANK);

        // 1000 토스뱅크 12자리 계좌번호 -> 최상위 정답 토스뱅크 검증
        List<BankName> tossResult = BankDetector.detectAll("100012345678");
        assertThat(tossResult.get(0)).isEqualTo(BankName.TOSS_BANK);

        // 1002 우리은행 13자리 계좌번호 -> 최상위 정답 우리은행 검증
        List<BankName> wooriResult = BankDetector.detectAll("1002123456789");
        assertThat(wooriResult.get(0)).isEqualTo(BankName.WOORI);

        // 301 농협 13자리 계좌번호 -> 최상위 정답 농협은행 검증
        List<BankName> nhResult = BankDetector.detectAll("3011234567890");
        assertThat(nhResult.get(0)).isEqualTo(BankName.NH);
    }
}
