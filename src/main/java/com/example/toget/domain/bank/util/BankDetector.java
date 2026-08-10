package com.example.toget.domain.bank.util;

import com.example.toget.global.enums.BankName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
     * 입력받은 계좌번호(하이픈 포함/미포함)를 분석하여 해당하는 BankName enum 단일값을 추론합니다 (하위 호환용).
     *
     * @param rawAccountNumber 계좌번호 문자열
     * @return 추론된 BankName (추론 불가능 시 Optional.empty())
     */
    public static Optional<BankName> detect(String rawAccountNumber) {
        List<BankName> detected = detectAll(rawAccountNumber);
        return detected.isEmpty() ? Optional.empty() : Optional.of(detected.get(0));
    }

    /**
     * 입력받은 계좌번호(하이픈 포함/미포함)를 분석하여 해당하는 모든 가용 BankName enum 목록을 추론합니다.
     *
     * @param rawAccountNumber 계좌번호 문자열
     * @return 추론된 BankName 목록 (추론 불가능 시 빈 리스트)
     */
    public static List<BankName> detectAll(String rawAccountNumber) {
        if (rawAccountNumber == null) {
            return List.of();
        }

        // 숫자만 정제
        String clean = rawAccountNumber.replaceAll("[^0-9]", "");
        int len = clean.length();

        if (len < 7 || len > 19) {
            return List.of();
        }

        Set<BankName> candidates = new LinkedHashSet<>();

        // 1. 카카오뱅크 (KAKAO_BANK) - 13자리 3333... 또는 33...
        if (len == 13 && (clean.startsWith("3333") || clean.startsWith("33"))) {
            candidates.add(BankName.KAKAO_BANK);
        }

        // 2. 토스뱅크 (TOSS_BANK) - 12자리 1000..., 1900..., 17..., 19...
        if (len == 12 && (clean.startsWith("1000") || clean.startsWith("1900") || clean.startsWith("17") || clean.startsWith("19"))) {
            candidates.add(BankName.TOSS_BANK);
        }

        // 3. 케이뱅크 (K_BANK) - 10자리(9로 시작), 12자리, 13자리(휴대폰 010), 14자리(70/79/90/7/9 등)
        if (len == 10 && clean.startsWith("9")) {
            candidates.add(BankName.K_BANK);
        }
        if (len == 12) {
            candidates.add(BankName.K_BANK);
        }
        if (len == 13 && clean.startsWith("010")) {
            candidates.add(BankName.K_BANK);
        }
        if (len == 14 && (clean.startsWith("70") || clean.startsWith("79") || clean.startsWith("90") || clean.startsWith("7") || clean.startsWith("9"))) {
            candidates.add(BankName.K_BANK);
        }

        // 4. 우리은행 (WOORI) - 13자리 1002..., 1005..., 1006..., 1007... / 11자리(상업) / 12자리(평화) / 14자리(한일)
        if (len == 13 && (clean.startsWith("1002") || clean.startsWith("1005") || clean.startsWith("1006") || clean.startsWith("1007") || clean.startsWith("1004") || clean.startsWith("1003"))) {
            candidates.add(BankName.WOORI);
        }
        if (len == 11) {
            String sub = clean.substring(3, 5);
            if (Set.of("05", "06", "07", "08", "02", "01", "04").contains(sub)) {
                candidates.add(BankName.WOORI);
            }
        }
        if (len == 12) {
            String sub = clean.substring(3, 5);
            if (Set.of("01", "21", "24", "05", "04", "25", "09").contains(sub)) {
                candidates.add(BankName.WOORI);
            }
        }
        if (len == 14) {
            String sub = clean.substring(8, 10);
            if (Set.of("01", "15", "02", "12", "04", "03", "13").contains(sub)) {
                candidates.add(BankName.WOORI);
            }
        }

        // 5. IBK기업은행 (IBK) - 10, 11, 12, 14자리
        if (len == 10 || len == 11) {
            candidates.add(BankName.IBK);
        }
        if (len == 12) {
            String subject2 = clean.substring(3, 5);
            Set<String> ibkSubjects = Set.of("01", "02", "03", "13", "07", "06", "04");
            if (ibkSubjects.contains(subject2)) {
                candidates.add(BankName.IBK);
            }
        }
        if (len == 14) {
            String subject2 = clean.substring(6, 8);
            Set<String> ibkSubjects = Set.of("01", "02", "03", "13", "07", "06", "04");
            if (ibkSubjects.contains(subject2)) {
                candidates.add(BankName.IBK);
            }
        }

        // 6. iM뱅크 (IM_BANK - 구 대구은행) - 7~11자리, 12, 13, 14자리
        if (len >= 7 && len <= 11) {
            candidates.add(BankName.IM_BANK);
        }
        if (len == 12 && (clean.substring(3, 5).equals("05") || clean.substring(3, 5).equals("08") || clean.substring(3, 5).equals("07") || clean.substring(3, 5).equals("02") || clean.substring(3, 5).equals("01") || clean.substring(3, 5).equals("04"))) {
            candidates.add(BankName.IM_BANK);
        }
        if (len == 13 && (clean.startsWith("91") || clean.startsWith("92") || clean.startsWith("93") || clean.startsWith("94") || clean.startsWith("96"))) {
            candidates.add(BankName.IM_BANK);
        }
        if (len == 14) {
            candidates.add(BankName.IM_BANK);
        }

        // 7. 광주은행 (GWANGJU) - 12, 13자리
        if (len == 12 && (clean.startsWith("107") || clean.startsWith("108") || clean.startsWith("109") || clean.startsWith("121") || clean.startsWith("123") || clean.startsWith("124") || clean.startsWith("122") || clean.startsWith("103") || clean.startsWith("101") || clean.startsWith("127"))) {
            candidates.add(BankName.GWANGJU);
        }
        if (len == 13 && (clean.substring(1, 4).equals("107") || clean.substring(1, 4).equals("109") || clean.substring(1, 4).equals("121") || clean.substring(1, 4).equals("103") || clean.substring(1, 4).equals("101") || clean.substring(1, 4).equals("127"))) {
            candidates.add(BankName.GWANGJU);
        }

        // 8. SC제일은행 (SC) - 11, 14자리
        if (len == 11 && (clean.startsWith("10") || clean.startsWith("20") || clean.startsWith("30") || clean.startsWith("15"))) {
            candidates.add(BankName.SC);
        }
        if (len == 14) {
            candidates.add(BankName.SC);
        }

        // 9. 한국씨티은행 (CITI) - 10, 11, 12, 13자리
        if (len == 10 || len == 12 || len == 13) {
            candidates.add(BankName.CITI);
        }
        if (len == 11 && (clean.startsWith("21") || clean.startsWith("22") || clean.startsWith("23") || clean.startsWith("24") || clean.startsWith("25"))) {
            candidates.add(BankName.CITI);
        }

        // 10. KDB산업은행 (KDB) - 11, 14자리
        if (len == 11 && (clean.startsWith("013") || clean.startsWith("020") || clean.startsWith("019") || clean.startsWith("011") || clean.startsWith("022") || Set.of("13","20","19","11","22").contains(clean.substring(3,5)))) {
            candidates.add(BankName.KDB);
        }
        if (len == 14 && (clean.startsWith("013") || clean.startsWith("020") || clean.startsWith("019") || clean.startsWith("011") || clean.startsWith("022") || clean.startsWith("010"))) {
            candidates.add(BankName.KDB);
        }

        // 11. 우체국 (POST_OFFICE) - 12, 13, 14자리
        if (len == 12 && (clean.startsWith("100") || clean.startsWith("530") || clean.startsWith("190") || clean.startsWith("110") || clean.startsWith("120"))) {
            candidates.add(BankName.POST_OFFICE);
        }
        if (len == 13 && (clean.startsWith("8") || clean.startsWith("9"))) {
            candidates.add(BankName.POST_OFFICE);
        }
        if (len == 14) {
            candidates.add(BankName.POST_OFFICE);
        }

        // 12. 신한은행 (SHINHAN) - 11, 12, 13, 14자리
        if (len == 11 && Set.of("01","09","61","04","05","06","08","02","07","03","99").contains(clean.substring(3,5))) {
            candidates.add(BankName.SHINHAN);
        }
        if (len == 12) {
            int prefix3 = Integer.parseInt(clean.substring(0, 3));
            if ((prefix3 >= 100 && prefix3 <= 139) || (prefix3 >= 140 && prefix3 <= 161)) {
                candidates.add(BankName.SHINHAN);
            }
        }
        if (len == 13 && (clean.startsWith("81") || clean.startsWith("82"))) {
            candidates.add(BankName.SHINHAN);
        }
        if (len == 14 && (clean.startsWith("560") || clean.startsWith("561") || clean.startsWith("562") || clean.startsWith("901"))) {
            candidates.add(BankName.SHINHAN);
        }

        // 13. KB국민은행 (KB) - 10, 11, 12, 14자리
        if (len == 10 && clean.startsWith("0")) {
            candidates.add(BankName.KB);
        }
        if (len == 11 && (clean.startsWith("0") || clean.startsWith("9"))) {
            candidates.add(BankName.KB);
        }
        if (len == 12) {
            String subject2 = clean.substring(3, 5);
            Set<String> kbSubjects = Set.of("01", "02", "03", "13", "07", "06", "04", "21", "24", "05", "25", "26", "18");
            if (kbSubjects.contains(subject2)) {
                candidates.add(BankName.KB);
            }
        }
        if (len == 14 && (clean.substring(4, 6).equals("92") || clean.substring(0, 2).equals("92") || clean.substring(4, 6).equals("01") || clean.substring(4, 6).equals("02") || clean.substring(4, 6).equals("25") || clean.substring(4, 6).equals("37") || clean.substring(4, 6).equals("90"))) {
            candidates.add(BankName.KB);
        }

        // 14. NH농협은행 (NH) - 11, 12, 13, 14자리
        if (len == 11 || len == 12) {
            String sub = clean.substring(3, 5);
            if (Set.of("01","02","12","06","05","17").contains(sub)) {
                candidates.add(BankName.NH);
            }
        }
        if (len == 13) {
            String prefix3 = clean.substring(0, 3);
            Set<String> nhPrefixes = Set.of("301", "302", "312", "306", "305", "317", "351", "352", "356", "355");
            if (nhPrefixes.contains(prefix3)) {
                candidates.add(BankName.NH);
            }
        }
        if (len == 14 && (clean.startsWith("790") || clean.startsWith("791") || clean.startsWith("792") || clean.startsWith("64") || clean.startsWith("65") || clean.startsWith("51") || clean.startsWith("52") || clean.startsWith("56") || clean.startsWith("55") || clean.startsWith("66") || clean.startsWith("67"))) {
            candidates.add(BankName.NH);
        }

        // 15. 하나은행 (HANA) - 11, 12, 14자리
        if (len == 12) {
            String prefix3 = clean.substring(0, 3);
            Set<String> hanaPrefixes = Set.of("611", "620", "600", "601", "630", "621", "631", "610");
            if (hanaPrefixes.contains(prefix3)) {
                candidates.add(BankName.HANA);
            }
        }
        if (len == 11) {
            String subject2 = clean.substring(3, 5);
            Set<String> hana11Subjects = Set.of("13", "33", "18", "38", "19", "39", "26", "11", "22");
            if (hana11Subjects.contains(subject2)) {
                candidates.add(BankName.HANA);
            }
        }
        if (len == 14) {
            candidates.add(BankName.HANA);
        }

        // 16. 새마을금고 (MG_SAEMAEUL) - 13자리
        if (len == 13 && (clean.startsWith("9") || clean.startsWith("09") || clean.startsWith("10") || clean.startsWith("13") || clean.startsWith("002") || clean.startsWith("003") || clean.startsWith("004") || clean.startsWith("005"))) {
            candidates.add(BankName.MG_SAEMAEUL);
        }

        // 17. 수협은행 (SUHYUP) - 11, 12, 14자리
        if (len == 11) {
            String sub = clean.substring(3, 5);
            Set<String> suhyupCentral = Set.of("43", "44", "45", "47", "49", "59", "61", "62", "63", "64", "66", "67", "68", "74", "75", "78", "81", "82", "83", "84", "85", "93");
            if (!suhyupCentral.contains(sub)) {
                candidates.add(BankName.SUHYUP);
            }
        }
        if (len == 12) {
            candidates.add(BankName.SUHYUP);
        }
        if (len == 14) {
            candidates.add(BankName.SUHYUP);
        }

        // 18. 부산은행 (BUSAN) - 12, 13자리
        if (len == 12) {
            String sub = clean.substring(3, 5);
            if (Set.of("01","02","12","03","09","13").contains(sub)) {
                candidates.add(BankName.BUSAN);
            }
        }
        if (len == 13 && (clean.startsWith("101") || clean.startsWith("102") || clean.startsWith("112") || clean.startsWith("103") || clean.startsWith("109") || clean.startsWith("113"))) {
            candidates.add(BankName.BUSAN);
        }

        // 19. 경남은행 (GYEONGNAM) - 12, 13자리
        if (len == 12) {
            String sub = clean.substring(3, 5);
            if (Set.of("07","09","21","22","03","01","35").contains(sub)) {
                candidates.add(BankName.GYEONGNAM);
            }
        }
        if (len == 13 && (clean.startsWith("207") || clean.startsWith("209") || clean.startsWith("221") || clean.startsWith("222") || clean.startsWith("203") || clean.startsWith("201") || clean.startsWith("235"))) {
            candidates.add(BankName.GYEONGNAM);
        }

        // 20. 신협 (SHINHYUP) - 10, 11, 12, 13, 14자리
        if (len == 10 || len == 11 || len == 14) {
            candidates.add(BankName.SHINHYUP);
        }
        if (len == 12) {
            candidates.add(BankName.SHINHYUP);
        }
        if (len == 13 && (clean.startsWith("131") || clean.startsWith("132") || clean.startsWith("135") || clean.startsWith("137") || clean.startsWith("12") || clean.startsWith("13"))) {
            candidates.add(BankName.SHINHYUP);
        }

        // 21. 전북은행 (JEONBUK) - 12, 13자리
        if (len == 12) {
            String sub = clean.substring(3, 5);
            if (Set.of("02","13","15","21","22","35","37","03","12","01","11","23","36").contains(sub)) {
                candidates.add(BankName.JEONBUK);
            }
        }
        if (len == 13 && (clean.startsWith("501") || clean.startsWith("502") || clean.startsWith("513") || clean.startsWith("522") || clean.startsWith("538") || clean.startsWith("013") || clean.startsWith("021") || clean.startsWith("012") || clean.startsWith("011") || clean.startsWith("023"))) {
            candidates.add(BankName.JEONBUK);
        }

        // 22. 제주은행 (JEJU) - 10, 12자리
        if (len == 10) {
            candidates.add(BankName.JEJU);
        }
        if (len == 12 && (clean.startsWith("010") || clean.startsWith("020") || clean.startsWith("030") || clean.startsWith("040") || clean.startsWith("050") || clean.startsWith("700") || clean.startsWith("770") || clean.startsWith("769") || clean.startsWith("711") || clean.startsWith("712") || clean.startsWith("713") || clean.startsWith("714") || clean.startsWith("707"))) {
            candidates.add(BankName.JEJU);
        }

        return new ArrayList<>(candidates);
    }
}


