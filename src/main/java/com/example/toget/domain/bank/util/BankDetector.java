package com.example.toget.domain.bank.util;

import com.example.toget.global.enums.BankName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * CMS 계좌번호 체계(2026.05.08 기준) 및 주요 은행별 과목코드/자릿수/프리픽스 패턴 기반
 * 계좌번호 은행 추론 엔진.
 */
public class BankDetector {

    /**
     * 계좌번호 추론 규칙의 신뢰도 수준.
     */
    public enum Confidence {
        /** 자릿수 + 프리픽스/과목코드 정밀 일치 (우선적용) */
        HIGH,
        /** 자릿수 중심 범용 매칭 (하위 범주 Fallback) */
        LOW
    }

    /**
     * 은행 추론 규칙 데이터 구조.
     */
    private record DetectionRule(BankName bankName, Confidence confidence, Predicate<String> matcher) {}

    private static final List<DetectionRule> RULES = createRules();

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
     * <p>
     * 고신뢰(HIGH) 매칭 결과가 1개 이상 존재할 경우 저신뢰(LOW) 결과는 자동으로 제외하여 추론 정확도를 유지합니다.
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

        Set<BankName> highCandidates = new LinkedHashSet<>();
        Set<BankName> lowCandidates = new LinkedHashSet<>();

        for (DetectionRule rule : RULES) {
            if (rule.matcher().test(clean)) {
                if (rule.confidence() == Confidence.HIGH) {
                    highCandidates.add(rule.bankName());
                } else {
                    lowCandidates.add(rule.bankName());
                }
            }
        }

        if (!highCandidates.isEmpty()) {
            return new ArrayList<>(highCandidates);
        }
        return new ArrayList<>(lowCandidates);
    }

    /**
     * 22개 은행에 대한 계좌번호 패턴 검증 규칙 테이블을 생성합니다.
     */
    private static List<DetectionRule> createRules() {
        List<DetectionRule> rules = new ArrayList<>();

        // 1. 카카오뱅크 (KAKAO_BANK) - 13자리 3333... 또는 33...
        rules.add(new DetectionRule(BankName.KAKAO_BANK, Confidence.HIGH,
                clean -> clean.length() == 13 && (clean.startsWith("3333") || clean.startsWith("33"))));

        // 2. 토스뱅크 (TOSS_BANK) - 12자리 1000..., 1900..., 17..., 19...
        rules.add(new DetectionRule(BankName.TOSS_BANK, Confidence.HIGH,
                clean -> clean.length() == 12 && (clean.startsWith("1000") || clean.startsWith("1900") || clean.startsWith("17") || clean.startsWith("19"))));

        // 3. 케이뱅크 (K_BANK) - 10자리(9로 시작), 12자리, 13자리(휴대폰 010), 14자리(70/79/90/7/9 등)
        rules.add(new DetectionRule(BankName.K_BANK, Confidence.HIGH, clean -> clean.length() == 10 && clean.startsWith("9")));
        rules.add(new DetectionRule(BankName.K_BANK, Confidence.LOW, clean -> clean.length() == 12));
        rules.add(new DetectionRule(BankName.K_BANK, Confidence.HIGH, clean -> clean.length() == 13 && clean.startsWith("010")));
        rules.add(new DetectionRule(BankName.K_BANK, Confidence.HIGH, clean -> clean.length() == 14 && (clean.startsWith("70") || clean.startsWith("79") || clean.startsWith("90") || clean.startsWith("7") || clean.startsWith("9"))));

        // 4. 우리은행 (WOORI) - 13자리 1002..., 1005... / 11자리(상업) / 12자리(평화) / 14자리(한일)
        rules.add(new DetectionRule(BankName.WOORI, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("1002") || clean.startsWith("1005") || clean.startsWith("1006") || clean.startsWith("1007") || clean.startsWith("1004") || clean.startsWith("1003"))));
        rules.add(new DetectionRule(BankName.WOORI, Confidence.LOW, clean -> clean.length() == 11 && Set.of("05", "06", "07", "08", "02", "01", "04").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.WOORI, Confidence.LOW, clean -> clean.length() == 12 && Set.of("01", "21", "24", "05", "04", "25", "09").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.WOORI, Confidence.LOW, clean -> clean.length() == 14 && Set.of("01", "15", "02", "12", "04", "03", "13").contains(clean.substring(8, 10))));

        // 5. IBK기업은행 (IBK) - 10, 11, 12, 14자리
        rules.add(new DetectionRule(BankName.IBK, Confidence.LOW, clean -> clean.length() == 10 || clean.length() == 11));
        rules.add(new DetectionRule(BankName.IBK, Confidence.LOW, clean -> clean.length() == 12 && Set.of("01", "02", "03", "13", "07", "06", "04").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.IBK, Confidence.LOW, clean -> clean.length() == 14 && Set.of("01", "02", "03", "13", "07", "06", "04").contains(clean.substring(6, 8))));

        // 6. iM뱅크 (IM_BANK - 구 대구은행) - 7~11자리, 12, 13, 14자리
        rules.add(new DetectionRule(BankName.IM_BANK, Confidence.LOW, clean -> clean.length() >= 7 && clean.length() <= 11));
        rules.add(new DetectionRule(BankName.IM_BANK, Confidence.LOW, clean -> clean.length() == 12 && Set.of("05", "08", "07", "02", "01", "04").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.IM_BANK, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("91") || clean.startsWith("92") || clean.startsWith("93") || clean.startsWith("94") || clean.startsWith("96"))));
        rules.add(new DetectionRule(BankName.IM_BANK, Confidence.LOW, clean -> clean.length() == 14));

        // 7. 광주은행 (GWANGJU) - 12, 13자리
        rules.add(new DetectionRule(BankName.GWANGJU, Confidence.HIGH, clean -> clean.length() == 12 && (clean.startsWith("107") || clean.startsWith("108") || clean.startsWith("109") || clean.startsWith("121") || clean.startsWith("123") || clean.startsWith("124") || clean.startsWith("122") || clean.startsWith("103") || clean.startsWith("101") || clean.startsWith("127"))));
        rules.add(new DetectionRule(BankName.GWANGJU, Confidence.HIGH, clean -> clean.length() == 13 && Set.of("107", "109", "121", "103", "101", "127").contains(clean.substring(1, 4))));

        // 8. SC제일은행 (SC) - 11, 14자리
        rules.add(new DetectionRule(BankName.SC, Confidence.HIGH, clean -> clean.length() == 11 && (clean.startsWith("10") || clean.startsWith("20") || clean.startsWith("30") || clean.startsWith("15"))));
        rules.add(new DetectionRule(BankName.SC, Confidence.LOW, clean -> clean.length() == 14));

        // 9. 한국씨티은행 (CITI) - 10, 11, 12, 13자리
        rules.add(new DetectionRule(BankName.CITI, Confidence.LOW, clean -> clean.length() == 10 || clean.length() == 12 || clean.length() == 13));
        rules.add(new DetectionRule(BankName.CITI, Confidence.HIGH, clean -> clean.length() == 11 && (clean.startsWith("21") || clean.startsWith("22") || clean.startsWith("23") || clean.startsWith("24") || clean.startsWith("25"))));

        // 10. KDB산업은행 (KDB) - 11, 14자리
        rules.add(new DetectionRule(BankName.KDB, Confidence.HIGH, clean -> clean.length() == 11 && (clean.startsWith("013") || clean.startsWith("020") || clean.startsWith("019") || clean.startsWith("011") || clean.startsWith("022"))));
        rules.add(new DetectionRule(BankName.KDB, Confidence.LOW, clean -> clean.length() == 11 && Set.of("13","20","19","11","22").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.KDB, Confidence.HIGH, clean -> clean.length() == 14 && (clean.startsWith("013") || clean.startsWith("020") || clean.startsWith("019") || clean.startsWith("011") || clean.startsWith("022") || clean.startsWith("010"))));

        // 11. 우체국 (POST_OFFICE) - 12, 13, 14자리
        rules.add(new DetectionRule(BankName.POST_OFFICE, Confidence.HIGH, clean -> clean.length() == 12 && (clean.startsWith("530") || clean.startsWith("190") || (clean.startsWith("100") && !clean.startsWith("1000")) || clean.startsWith("120"))));
        rules.add(new DetectionRule(BankName.POST_OFFICE, Confidence.LOW, clean -> clean.length() == 12 && clean.startsWith("110")));
        rules.add(new DetectionRule(BankName.POST_OFFICE, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("8") || clean.startsWith("9"))));
        rules.add(new DetectionRule(BankName.POST_OFFICE, Confidence.LOW, clean -> clean.length() == 14));

        // 12. 신한은행 (SHINHAN) - 11, 12, 13, 14자리
        rules.add(new DetectionRule(BankName.SHINHAN, Confidence.LOW, clean -> clean.length() == 11 && Set.of("01","09","61","04","05","06","08","02","07","03","99").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.SHINHAN, Confidence.HIGH, clean -> {
            if (clean.length() != 12 || clean.startsWith("1000")) return false;
            int p3 = Integer.parseInt(clean.substring(0, 3));
            return (p3 >= 100 && p3 <= 139) || (p3 >= 140 && p3 <= 161);
        }));

        rules.add(new DetectionRule(BankName.SHINHAN, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("81") || clean.startsWith("82"))));
        rules.add(new DetectionRule(BankName.SHINHAN, Confidence.HIGH, clean -> clean.length() == 14 && (clean.startsWith("560") || clean.startsWith("561") || clean.startsWith("562") || clean.startsWith("901"))));

        // 13. KB국민은행 (KB) - 10, 11, 12, 14자리
        rules.add(new DetectionRule(BankName.KB, Confidence.HIGH, clean -> clean.length() == 10 && clean.startsWith("0")));
        rules.add(new DetectionRule(BankName.KB, Confidence.HIGH, clean -> clean.length() == 11 && (clean.startsWith("0") || clean.startsWith("9"))));
        rules.add(new DetectionRule(BankName.KB, Confidence.LOW, clean -> clean.length() == 12 && Set.of("01", "02", "03", "13", "07", "06", "04", "21", "24", "05", "25", "26", "18").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.KB, Confidence.HIGH, clean -> clean.length() == 14 && (clean.substring(4, 6).equals("92") || clean.substring(0, 2).equals("92") || clean.substring(4, 6).equals("01") || clean.substring(4, 6).equals("02") || clean.substring(4, 6).equals("25") || clean.substring(4, 6).equals("37") || clean.substring(4, 6).equals("90"))));

        // 14. NH농협은행 (NH) - 11, 12, 13, 14자리
        rules.add(new DetectionRule(BankName.NH, Confidence.LOW, clean -> (clean.length() == 11 || clean.length() == 12) && Set.of("01","02","12","06","05","17").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.NH, Confidence.HIGH, clean -> clean.length() == 13 && Set.of("301", "302", "312", "306", "305", "317", "351", "352", "356", "355").contains(clean.substring(0, 3))));
        rules.add(new DetectionRule(BankName.NH, Confidence.HIGH, clean -> clean.length() == 14 && (clean.startsWith("790") || clean.startsWith("791") || clean.startsWith("792") || clean.startsWith("64") || clean.startsWith("65") || clean.startsWith("51") || clean.startsWith("52") || clean.startsWith("56") || clean.startsWith("55") || clean.startsWith("66") || clean.startsWith("67"))));

        // 15. 하나은행 (HANA) - 11, 12, 14자리
        rules.add(new DetectionRule(BankName.HANA, Confidence.HIGH, clean -> clean.length() == 12 && Set.of("611", "620", "600", "601", "630", "621", "631", "610").contains(clean.substring(0, 3))));
        rules.add(new DetectionRule(BankName.HANA, Confidence.LOW, clean -> clean.length() == 11 && Set.of("13", "33", "18", "38", "19", "39", "26", "11", "22").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.HANA, Confidence.LOW, clean -> clean.length() == 14));

        // 16. 새마을금고 (MG_SAEMAEUL) - 13자리
        rules.add(new DetectionRule(BankName.MG_SAEMAEUL, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("9") || clean.startsWith("09") || (clean.startsWith("10") && !clean.startsWith("100")) || clean.startsWith("13") || clean.startsWith("002") || clean.startsWith("003") || clean.startsWith("004") || clean.startsWith("005"))));


        // 17. 수협은행 (SUHYUP) - 11, 12, 14자리
        rules.add(new DetectionRule(BankName.SUHYUP, Confidence.HIGH, clean -> clean.length() == 11 && !Set.of("43", "44", "45", "47", "49", "59", "61", "62", "63", "64", "66", "67", "68", "74", "75", "78", "81", "82", "83", "84", "85", "93").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.SUHYUP, Confidence.LOW, clean -> clean.length() == 12 || clean.length() == 14));


        // 18. 부산은행 (BUSAN) - 12, 13자리
        rules.add(new DetectionRule(BankName.BUSAN, Confidence.LOW, clean -> clean.length() == 12 && Set.of("01","02","12","03","09","13").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.BUSAN, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("101") || clean.startsWith("102") || clean.startsWith("112") || clean.startsWith("103") || clean.startsWith("109") || clean.startsWith("113"))));

        // 19. 경남은행 (GYEONGNAM) - 12, 13자리
        rules.add(new DetectionRule(BankName.GYEONGNAM, Confidence.LOW, clean -> clean.length() == 12 && Set.of("07","09","21","22","03","01","35").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.GYEONGNAM, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("207") || clean.startsWith("209") || clean.startsWith("221") || clean.startsWith("222") || clean.startsWith("203") || clean.startsWith("201") || clean.startsWith("235"))));

        // 20. 신협 (SHINHYUP) - 10, 11, 12, 13, 14자리
        rules.add(new DetectionRule(BankName.SHINHYUP, Confidence.LOW, clean -> clean.length() == 10 || clean.length() == 11 || clean.length() == 12 || clean.length() == 14));
        rules.add(new DetectionRule(BankName.SHINHYUP, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("131") || clean.startsWith("132") || clean.startsWith("135") || clean.startsWith("137") || clean.startsWith("12") || clean.startsWith("13"))));

        // 21. 전북은행 (JEONBUK) - 12, 13자리
        rules.add(new DetectionRule(BankName.JEONBUK, Confidence.LOW, clean -> clean.length() == 12 && Set.of("02","13","15","21","22","35","37","03","12","01","11","23","36").contains(clean.substring(3, 5))));
        rules.add(new DetectionRule(BankName.JEONBUK, Confidence.HIGH, clean -> clean.length() == 13 && (clean.startsWith("501") || clean.startsWith("502") || clean.startsWith("513") || clean.startsWith("522") || clean.startsWith("538") || clean.startsWith("013") || clean.startsWith("021") || clean.startsWith("012") || clean.startsWith("011") || clean.startsWith("023"))));

        // 22. 제주은행 (JEJU) - 10, 12자리
        rules.add(new DetectionRule(BankName.JEJU, Confidence.LOW, clean -> clean.length() == 10));
        rules.add(new DetectionRule(BankName.JEJU, Confidence.HIGH, clean -> clean.length() == 12 && (clean.startsWith("010") || clean.startsWith("020") || clean.startsWith("030") || clean.startsWith("040") || clean.startsWith("050") || clean.startsWith("700") || clean.startsWith("770") || clean.startsWith("769") || clean.startsWith("711") || clean.startsWith("712") || clean.startsWith("713") || clean.startsWith("714") || clean.startsWith("707"))));


        return rules;
    }
}



