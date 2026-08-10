package com.example.toget.domain.bank.util;

import com.example.toget.global.enums.BankName;
import java.util.Optional;
import java.util.Set;

/**
 * CMS 계좌번호 체계(2026.05.08 기준) 및 주요 은행별 과목코드/자릿수/프리픽스 패턴 기반
 * 계좌번호 은행 추론 엔진.
 */
public class BankDetector {

    private BankDetector() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 입력받은 계좌번호(하이픈 포함/미포함)를 분석하여 해당하는 BankName enum을 추론합니다.
     *
     * @param rawAccountNumber 계좌번호 문자열
     * @return 추론된 BankName (추론 불가능 시 Optional.empty())
     */
    public static Optional<BankName> detect(String rawAccountNumber) {
        if (rawAccountNumber == null) {
            return Optional.empty();
        }

        // 숫자만 정제
        String clean = rawAccountNumber.replaceAll("[^0-9]", "");
        int len = clean.length();

        if (len < 7 || len > 19) {
            return Optional.empty();
        }

        // 1. 카카오뱅크 (KAKAO_BANK) - 13자리 3333 시작 (우선순위 높음)
        if (len == 13 && clean.startsWith("3333")) {
            return Optional.of(BankName.KAKAO_BANK);
        }

        // 2. 토스뱅크 (TOSS_BANK) - 12자리 1000..., 1900..., 100..., 150...
        if (len == 12 && (clean.startsWith("1000") || clean.startsWith("1900") || clean.startsWith("100") || clean.startsWith("150"))) {
            return Optional.of(BankName.TOSS_BANK);
        }

        // 3. 케이뱅크 (K_BANK) - 10자리(9로 시작), 13자리(휴대폰 010), 14자리(70/79/90 등)
        if (len == 10 && clean.startsWith("9")) {
            return Optional.of(BankName.K_BANK);
        }
        if (len == 13 && clean.startsWith("010")) {
            return Optional.of(BankName.K_BANK);
        }
        if (len == 14 && (clean.startsWith("70") || clean.startsWith("79") || clean.startsWith("90"))) {
            return Optional.of(BankName.K_BANK);
        }

        // 4. 우리은행 (WOORI) - 13자리 1002..., 1005..., 1006..., 1007...
        if (len == 13 && (clean.startsWith("1002") || clean.startsWith("1005") || clean.startsWith("1006") || clean.startsWith("1007") || clean.startsWith("1004") || clean.startsWith("1003"))) {
            return Optional.of(BankName.WOORI);
        }

        // 5. 신한은행 (SHINHAN) - 12자리 과목 110~139, 100~109, 140~149, 150~154, 155~159, 160~161
        if (len == 12) {
            int prefix3 = Integer.parseInt(clean.substring(0, 3));
            if ((prefix3 >= 100 && prefix3 <= 139) || (prefix3 >= 140 && prefix3 <= 161)) {
                return Optional.of(BankName.SHINHAN);
            }
        }
        if (len == 14 && (clean.startsWith("560") || clean.startsWith("561") || clean.startsWith("562") || clean.startsWith("901"))) {
            return Optional.of(BankName.SHINHAN);
        }

        // 6. KB국민은행 (KB)
        // 10자리 0-고객지정번호
        if (len == 10 && clean.startsWith("0")) {
            return Optional.of(BankName.KB);
        }
        // 11자리 0 또는 9 고객지정
        if (len == 11 && (clean.startsWith("0") || clean.startsWith("9"))) {
            return Optional.of(BankName.KB);
        }
        // 12자리 점3-과목2-일련6-검증1 중 과목코드 01,02,03,13,07,06,04,21,24,05,25,26, 06,18
        if (len == 12) {
            String subject2 = clean.substring(3, 5);
            Set<String> kbSubjects = Set.of("01", "02", "03", "13", "07", "06", "04", "21", "24", "05", "25", "26", "18");
            if (kbSubjects.contains(subject2)) {
                return Optional.of(BankName.KB);
            }
        }
        // 14자리 과목 92
        if (len == 14 && (clean.substring(4, 6).equals("92") || clean.substring(0, 2).equals("92"))) {
            return Optional.of(BankName.KB);
        }

        // 7. NH농협은행 (NH)
        if (len == 13) {
            String prefix3 = clean.substring(0, 3);
            Set<String> nhPrefixes = Set.of("301", "302", "312", "306", "305", "317", "351", "352", "356", "355");
            if (nhPrefixes.contains(prefix3)) {
                return Optional.of(BankName.NH);
            }
        }
        if (len == 14 && (clean.startsWith("790") || clean.startsWith("791") || clean.startsWith("792"))) {
            return Optional.of(BankName.NH);
        }

        // 8. 하나은행 (HANA)
        if (len == 12) {
            String prefix3 = clean.substring(0, 3);
            Set<String> hanaPrefixes = Set.of("611", "620", "600", "601", "630", "621", "631", "610");
            if (hanaPrefixes.contains(prefix3)) {
                return Optional.of(BankName.HANA);
            }
        }
        if (len == 11) {
            String subject2 = clean.substring(3, 5);
            Set<String> hana11Subjects = Set.of("13", "33", "18", "38", "19", "39", "26", "11", "22");
            if (hana11Subjects.contains(subject2)) {
                return Optional.of(BankName.HANA);
            }
        }

        // 9. IBK기업은행 (IBK)
        if (len == 12) {
            String subject2 = clean.substring(3, 5);
            Set<String> ibkSubjects = Set.of("01", "02", "03", "13", "07", "06", "04");
            if (ibkSubjects.contains(subject2)) {
                return Optional.of(BankName.IBK);
            }
        }

        // 10. 새마을금고 (MG_SAEMAEUL) - 13자리
        if (len == 13 && clean.startsWith("9") && !clean.startsWith("90")) {
            return Optional.of(BankName.MG_SAEMAEUL);
        }

        // 11. 수협은행 (SUHYUP)
        if (len == 11) {
            String sub = clean.substring(3, 5);
            Set<String> suhyupCentral = Set.of("43", "44", "45", "47", "49", "59", "61", "62", "63", "64", "66", "67", "68", "74", "75", "78", "81", "82", "83", "84", "85", "93");
            if (!suhyupCentral.contains(sub)) {
                // 수협은행 (007)
                return Optional.of(BankName.SUHYUP);
            }
        }

        // 12. iM뱅크 (IM_BANK - 구 대구은행)
        if (len == 12 && (clean.substring(3, 5).equals("05") || clean.substring(3, 5).equals("08") || clean.substring(3, 5).equals("07"))) {
            return Optional.of(BankName.IM_BANK);
        }

        // 13. 부산은행 (BUSAN)
        if (len == 13 && (clean.startsWith("101") || clean.startsWith("102") || clean.startsWith("112") || clean.startsWith("103") || clean.startsWith("109") || clean.startsWith("113"))) {
            return Optional.of(BankName.BUSAN);
        }

        // 14. 광주은행 (GWANGJU)
        if (len == 12 && (clean.startsWith("107") || clean.startsWith("108") || clean.startsWith("109") || clean.startsWith("121") || clean.startsWith("123") || clean.startsWith("124") || clean.startsWith("122"))) {
            return Optional.of(BankName.GWANGJU);
        }

        // 15. 경남은행 (GYEONGNAM)
        if (len == 13 && (clean.startsWith("207") || clean.startsWith("209") || clean.startsWith("221") || clean.startsWith("222") || clean.startsWith("203") || clean.startsWith("201") || clean.startsWith("235"))) {
            return Optional.of(BankName.GYEONGNAM);
        }

        // 16. KDB산업은행 (KDB)
        if (len == 11 && (clean.startsWith("013") || clean.startsWith("020") || clean.startsWith("019") || clean.startsWith("011") || clean.startsWith("022"))) {
            return Optional.of(BankName.KDB);
        }
        if (len == 14 && (clean.startsWith("013") || clean.startsWith("020") || clean.startsWith("019") || clean.startsWith("011") || clean.startsWith("022") || clean.startsWith("010"))) {
            return Optional.of(BankName.KDB);
        }

        // 17. SC제일은행 (SC)
        if (len == 11 && (clean.startsWith("10") || clean.startsWith("20") || clean.startsWith("30"))) {
            return Optional.of(BankName.SC);
        }

        // 18. 우체국 (POST_OFFICE)
        if (len == 12 && (clean.startsWith("100") || clean.startsWith("530") || clean.startsWith("190") || clean.startsWith("110") || clean.startsWith("120"))) {
            return Optional.of(BankName.POST_OFFICE);
        }

        // 기본 fallback: 13자리 3333 이외의 카카오뱅크 패턴
        if (len == 13 && clean.startsWith("33")) {
            return Optional.of(BankName.KAKAO_BANK);
        }

        return Optional.empty();
    }
}
