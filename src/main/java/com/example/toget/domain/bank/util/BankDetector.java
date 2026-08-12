package com.example.toget.domain.bank.util;

import com.example.toget.global.enums.BankName;

import java.util.*;
import java.util.function.Predicate;

/**
 * KFTC(금융결제원) CMS 계좌번호 체계 데이터(korean-bank-detector) 및 주요 22개 은행별
 * 과목코드/자릿수/프리픽스 패턴 기반 계좌번호 은행 추론 엔진.
 */
public class BankDetector {

    public enum Confidence {
        HIGH,
        LOW
    }

    public record YCodeRange(int from, int to) {
        public boolean contains(int val) {
            return val >= from && val <= to;
        }
    }

    public record YCodeSpec(List<String> exactCodes, List<YCodeRange> ranges) {
        public static YCodeSpec of(String... codes) {
            return new YCodeSpec(List.of(codes), List.of());
        }

        public static YCodeSpec of(List<String> codes, List<YCodeRange> ranges) {
            return new YCodeSpec(codes, ranges);
        }

        public boolean matches(String slice) {
            if (exactCodes != null && exactCodes.contains(slice)) {
                return true;
            }
            if (ranges != null && !ranges.isEmpty()) {
                try {
                    int val = Integer.parseInt(slice);
                    for (YCodeRange range : ranges) {
                        if (range.contains(val)) {
                            return true;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
            return false;
        }
    }

    public record PatternSpec(
            List<String> templates,
            YCodeSpec yCodes,
            List<Predicate<String>> additionalRules
    ) {
        public PatternSpec(List<String> templates, YCodeSpec yCodes) {
            this(templates, yCodes, List.of());
        }
    }

    private record InstitutionRule(
            BankName bankName,
            Confidence defaultConfidence,
            List<PatternSpec> patterns
    ) {}

    private record MatchResult(BankName bankName, int score, Confidence confidence) {}

    private static final List<InstitutionRule> RULES = createRules();

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
     * korean-bank-detector KFTC 패턴 기반 점수(Score) 계산 후 내림차순 정렬하여 반환합니다.
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

        List<MatchResult> matches = new ArrayList<>();

        for (InstitutionRule rule : RULES) {
            int maxInstScore = 0;
            for (PatternSpec pattern : rule.patterns()) {
                int score = scorePattern(pattern, clean);
                if (score > maxInstScore) {
                    maxInstScore = score;
                }
            }

            if (maxInstScore > 0) {
                matches.add(new MatchResult(rule.bankName(), maxInstScore, rule.defaultConfidence()));
            }
        }

        if (matches.isEmpty()) {
            return List.of();
        }

        // 점수 내림차순 정렬 (동점 시 선언 순서 보존)
        matches.sort((a, b) -> Integer.compare(b.score(), a.score()));

        // 중복 제거하면서 리스트 변환 (LinkedHashSet)
        Set<BankName> result = new LinkedHashSet<>();
        for (MatchResult m : matches) {
            result.add(m.bankName());
        }

        return new ArrayList<>(result);
    }

    private static boolean literalDigitsMatch(String strippedTemplate, String normalized) {
        for (int i = 0; i < strippedTemplate.length() && i < normalized.length(); i++) {
            char ch = strippedTemplate.charAt(i);
            if (ch >= '0' && ch <= '9') {
                if (normalized.charAt(i) != ch) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int scorePattern(PatternSpec pattern, String normalized) {
        int maxScore = 0;

        for (String template : pattern.templates()) {
            int patternScore = 0;
            String stripped = template.replace("-", "").toUpperCase();

            // 1. 고정 숫자 위치 매칭 검증
            if (!literalDigitsMatch(stripped, normalized)) {
                continue;
            }

            // 2. Y-code 과목코드 매칭 검증
            int yStart = stripped.indexOf('Y');
            if (yStart >= 0) {
                int yEnd = yStart;
                while (yEnd < stripped.length() && stripped.charAt(yEnd) == 'Y') {
                    yEnd++;
                }

                if (pattern.yCodes() != null) {
                    if (normalized.length() >= yEnd) {
                        String slice = normalized.substring(yStart, yEnd);
                        if (pattern.yCodes().matches(slice)) {
                            patternScore += 1;
                        } else {
                            continue; // Y-code 불일치 시 0점 처리
                        }
                    } else {
                        continue;
                    }
                }
            }

            // 3. 자릿수 일치 검증
            if (stripped.length() == normalized.length()) {
                patternScore += 1;
            }

            if (patternScore > maxScore) {
                maxScore = patternScore;
            }
        }

        // 4. 추가 규칙 검증 (기본 점수가 1점 이상일 때 적용)
        int finalScore = maxScore;
        if (maxScore > 0 && pattern.additionalRules() != null && !pattern.additionalRules().isEmpty()) {
            for (Predicate<String> rule : pattern.additionalRules()) {
                if (rule.test(normalized)) {
                    finalScore += 1;
                }
            }
        }

        return finalScore;
    }

    /**
     * 22개 전체 지원 은행에 대한 KFTC CMS 검증 패턴 테이블 생성.
     */
    private static List<InstitutionRule> createRules() {
        List<InstitutionRule> rules = new ArrayList<>();

        // 1. 카카오뱅크 (KAKAO_BANK) - 090
        rules.add(new InstitutionRule(BankName.KAKAO_BANK, Confidence.HIGH, List.of(
                new PatternSpec(List.of("TYYY-ZZ-ZZZZZZZ"), YCodeSpec.of("333", "388", "355", "310"), List.of(n -> n.startsWith("3"))),
                new PatternSpec(List.of("TYYY-ZZ-ZZZZZZZ"), YCodeSpec.of("777", "979"), List.of(n -> n.startsWith("7"))),
                new PatternSpec(List.of("TYYY-ZZ-ZZZZZZZ"), YCodeSpec.of("101"), List.of(n -> n.startsWith("9"))),
                new PatternSpec(List.of("3333-ZZ-ZZZZZZZ", "33ZZ-ZZ-ZZZZZZZ"), null, List.of(n -> n.length() == 13 && n.startsWith("33")))
        )));

        // 2. 토스뱅크 (TOSS_BANK) - 092
        rules.add(new InstitutionRule(BankName.TOSS_BANK, Confidence.HIGH, List.of(
                new PatternSpec(List.of("YYYZ-ZZZZ-ZZZC"), YCodeSpec.of("100", "106", "300", "150", "700"), List.of(n -> n.length() >= 4 && (n.charAt(3) == '8' || n.charAt(3) == '0'))),
                new PatternSpec(List.of("17ZZ-ZZZZ-ZZZZ", "19ZZ-ZZZZ-ZZZZ"), null, List.of(n -> n.startsWith("17") || n.startsWith("19"))),
                new PatternSpec(List.of("1000-ZZZZ-ZZZZ", "1900-ZZZZ-ZZZZ"), null, List.of(n -> n.length() == 12 && (n.startsWith("1000") || n.startsWith("1900"))))
        )));

        // 3. 케이뱅크 (K_BANK) - 089
        rules.add(new InstitutionRule(BankName.K_BANK, Confidence.HIGH, List.of(
                new PatternSpec(List.of("YYY-YNN-NNZZZZ"), YCodeSpec.of("1002", "1005")),
                new PatternSpec(List.of("9ZZ-ZZZ-ZZZZ"), null, List.of(n -> n.length() == 10 && n.startsWith("9"))),
                new PatternSpec(List.of("010-ZZZZ-ZZZZZ"), null, List.of(n -> n.length() == 13 && n.startsWith("010"))),
                new PatternSpec(List.of("70ZZ-ZZZZ-ZZZZZ", "79ZZ-ZZZZ-ZZZZZ", "90ZZ-ZZZZ-ZZZZZ"), null, List.of(n -> n.length() == 14 && (n.startsWith("70") || n.startsWith("79") || n.startsWith("90"))))
        )));

        // 4. 신한은행 (SHINHAN) - 088
        rules.add(new InstitutionRule(BankName.SHINHAN, Confidence.HIGH, List.of(
                new PatternSpec(List.of("YYY-ZZZ-ZZZZZC"), YCodeSpec.of(
                        List.of("160", "161", "180", "298", "268", "269"),
                        List.of(new YCodeRange(100, 109), new YCodeRange(110, 139), new YCodeRange(140, 149), new YCodeRange(150, 154), new YCodeRange(155, 159))
                )),
                new PatternSpec(List.of("YYY-TTT-ZZZZZZZC"), YCodeSpec.of("560", "561", "562")),
                new PatternSpec(List.of("XXX-YY-ZZZZZC"), YCodeSpec.of("01", "02", "03", "04", "05", "06", "07", "08", "09", "11", "12", "13", "61", "99")),
                new PatternSpec(List.of("XXX-YY-ZZZZZZZC"), YCodeSpec.of("01", "02", "03", "04", "05", "06", "07", "08", "09", "61", "81", "82")),
                new PatternSpec(List.of("XXX-YYY-ZZZZZZZC"), YCodeSpec.of("901"))
        )));

        // 5. KB국민은행 (KB) - 004
        rules.add(new InstitutionRule(BankName.KB, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXXX-YY-ZZZZZC", "XXXX-YY-ZZZZZZZC"), YCodeSpec.of("01", "02", "06", "07", "18", "25", "37", "90")),
                new PatternSpec(List.of("XXX-YY-ZZZZ-ZZC", "XXXXYY-ZZ-ZZZZZC"), YCodeSpec.of("01", "02", "04", "05", "07", "24", "25", "26", "92"), List.of(n -> n.startsWith("0"))),
                new PatternSpec(List.of("0ZZ-ZZZ-ZZZZ"), null, List.of(n -> n.length() == 10 && n.startsWith("0"))),
                new PatternSpec(List.of("92ZZ-YY-ZZZZZZZZ"), YCodeSpec.of("92"), List.of(n -> n.length() == 14))
        )));

        // 6. 우리은행 (WOORI) - 020
        rules.add(new InstitutionRule(BankName.WOORI, Confidence.HIGH, List.of(
                new PatternSpec(List.of("SYYY-CZZ-ZZZZZZ"), YCodeSpec.of("002", "003", "004", "005", "006", "007"), List.of(n -> n.startsWith("1"))),
                new PatternSpec(List.of("XXX-BBBBBC-YY-ZZC"), YCodeSpec.of("18", "92")),
                new PatternSpec(List.of("XXX-YY-ZZZZZC"), YCodeSpec.of("002", "003", "004", "005", "006", "007")),
                new PatternSpec(List.of("XXX-BBBBBB-YY-ZZC"), YCodeSpec.of("01", "02", "03", "04", "12", "13", "15")),
                new PatternSpec(List.of("XXX-YY-ZZZZZZC"), YCodeSpec.of("01", "04", "05", "09", "21", "24", "25"))
        )));

        // 7. 하나은행 (HANA) - 081
        rules.add(new InstitutionRule(BankName.HANA, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YY-ZZZZZ-C"), YCodeSpec.of("11", "13", "15", "18", "19", "22", "23", "24", "26", "29", "33", "38", "39", "70", "73", "74", "75", "77")),
                new PatternSpec(List.of("YYY-ZZZZZZ-ZZC"), YCodeSpec.of(
                        List.of("600", "601", "610", "611", "620", "621", "630", "631", "700", "703", "704", "705", "707", "810", "811", "814", "815", "817", "818"),
                        List.of(new YCodeRange(710, 716))
                )),
                new PatternSpec(List.of("XXX-ZZZZZZ-ZZCYY"), YCodeSpec.of("01", "02", "04", "05", "07", "08", "32", "37", "60", "94"))
        )));

        // 8. NH농협은행 (NH) - 011 / 012
        rules.add(new InstitutionRule(BankName.NH, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YY-ZZZZZC", "XXXX-YY-ZZZZZC"), YCodeSpec.of("01", "02", "04", "05", "06", "10", "12", "14", "17", "21", "24", "28", "31", "34", "43", "45", "46", "47", "49", "51", "52", "55", "56", "59", "79", "80", "81", "86", "87", "88")),
                new PatternSpec(List.of("YYY-ZZZZ-ZZZZ-CT"), YCodeSpec.of("028", "031", "043", "046", "079", "081", "086", "087", "088", "301", "302", "304", "305", "306", "310", "312", "314", "317", "321", "324", "334", "345", "347", "349", "351", "352", "354", "355", "356", "359", "360", "380", "384", "394", "398")),
                new PatternSpec(List.of("XXXXXX-YY-ZZZZZC", "YYY-ZZZZ-ZZZZ-ZZC"), YCodeSpec.of("64", "65", "66", "67", "790", "791", "792"))
        )));

        // 9. IBK기업은행 (IBK) - 003
        rules.add(new InstitutionRule(BankName.IBK, Confidence.HIGH, List.of(
                new PatternSpec(List.of("BBBBBBBB-ZZ", "AAA-BBBBBBBB"), null, List.of(n -> n.length() == 10 || n.length() == 11)),
                new PatternSpec(List.of("XXX-YY-ZZZZZZC", "XXX-BBBBBB-YY-ZZC"), YCodeSpec.of("01", "02", "03", "04", "06", "07", "13"))
        )));

        // 10. SC제일은행 (SC) - 023
        rules.add(new InstitutionRule(BankName.SC, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YY-ZZZZZC"), YCodeSpec.of("10", "20", "30", "85")),
                new PatternSpec(List.of("XXX-YY-ZZZZZZZZC"), YCodeSpec.of("15", "16"))
        )));

        // 11. 한국씨티은행 (CITI) - 027
        rules.add(new InstitutionRule(BankName.CITI, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-ZZZZZ-YYC-ZZ"), YCodeSpec.of("01", "03", "05", "06", "07", "11", "13", "15", "21", "23", "24", "25", "26", "27", "29", "31", "33", "41", "42", "43", "51", "53", "55", "63", "71", "81", "99")),
                new PatternSpec(List.of("XX-YY-ZZZZZC", "Y-ZZZZZZ-ZZC"), YCodeSpec.of(
                        List.of("00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "20", "21", "30", "32", "33", "34", "35", "40", "41", "42", "46", "48", "59", "63", "64", "70", "71", "80", "81", "99"),
                        List.of(new YCodeRange(10, 19), new YCodeRange(36, 38), new YCodeRange(43, 45), new YCodeRange(50, 58), new YCodeRange(60, 69), new YCodeRange(72, 78), new YCodeRange(83, 88), new YCodeRange(91, 96))
                )),
                new PatternSpec(List.of("T-BBBBBB-CYY-ZZ"), YCodeSpec.of("18", "24", "25", "41"))
        )));

        // 12. 아이엠뱅크 (IM_BANK - 구 대구은행) - 031
        rules.add(new InstitutionRule(BankName.IM_BANK, Confidence.HIGH, List.of(
                new PatternSpec(List.of("YY-ZZZZZZZZZZZ", "XXX-YY-ZZZZZZC", "YYY-ZZ-ZZZZZZC", "XXX-YY-ZZZZZZ-ZZZ"), YCodeSpec.of(
                        List.of("01", "02", "04", "05", "06", "08", "13", "14", "19", "20", "21", "25", "27", "28", "96", "501", "502", "504", "505", "508", "519", "520", "521", "524", "525", "527", "528", "937"),
                        List.of(new YCodeRange(91, 94))
                ))
        )));

        // 13. 부산은행 (BUSAN) - 032
        rules.add(new InstitutionRule(BankName.BUSAN, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YYY-ZZZZZC", "ZYYY-ZZZ-ZZZZZZC"), YCodeSpec.of("101", "103", "107", "108", "109", "121", "122", "123", "124", "127", "716"))
        )));

        // 14. 새마을금고 (MG_SAEMAEUL) - 045
        rules.add(new InstitutionRule(BankName.MG_SAEMAEUL, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXXX-YY-ZZZZZZ-C"), YCodeSpec.of("09", "10", "13", "37")),
                new PatternSpec(List.of("XXXX-YYY-ZZZZZZ-C"), YCodeSpec.of(List.of(), List.of(new YCodeRange(801, 810), new YCodeRange(851, 860)))),
                new PatternSpec(List.of("9YYY-ZZZZ-ZZZZ-C"), YCodeSpec.of(
                        List.of("002", "003", "004", "005", "072", "090", "091", "092", "093", "200", "202", "205", "212"),
                        List.of(new YCodeRange(207, 210))
                ), List.of(n -> n.startsWith("9")))
        )));

        // 15. 수협은행 (SUHYUP) - 007
        rules.add(new InstitutionRule(BankName.SUHYUP, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YY-ZZZZZ-C"), YCodeSpec.of("01", "02", "06", "08")),
                new PatternSpec(List.of("YYYZ-ZZZZ-ZZZC"), YCodeSpec.of("101", "102", "103", "106", "108", "113", "114", "201", "202", "206", "208", "209"), List.of(n -> n.startsWith("0") && n.length() == 12)),
                new PatternSpec(List.of("XXX-YY-ZZZZZZZZ-C"), YCodeSpec.of("40"))
        )));

        // 16. KDB산업은행 (KDB) - 002
        rules.add(new InstitutionRule(BankName.KDB, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YY-ZZZZZC"), YCodeSpec.of("11", "13", "19", "20", "22")),
                new PatternSpec(List.of("YYY-ZZZZZZZC-XXX"), YCodeSpec.of("011", "013", "019", "020", "022"))
        )));

        // --- 6개 보존 은행 (우체국, 신협, 광주, 전북, 경남, 제주) ---

        // 17. 우체국 (POST_OFFICE) - 071
        rules.add(new InstitutionRule(BankName.POST_OFFICE, Confidence.HIGH, List.of(
                new PatternSpec(List.of("YYY-ZZ-ZZZZZZC", "YYY-ZZZZZZZZC"), YCodeSpec.of("100", "110", "120", "190", "530")),
                new PatternSpec(List.of("YYY-ZZZZ-ZZZZZ"), YCodeSpec.of("8", "9"), List.of(n -> n.length() == 13 && (n.startsWith("8") || n.startsWith("9"))))
        )));

        // 18. 신협 (SHINHYUP) - 048
        rules.add(new InstitutionRule(BankName.SHINHYUP, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YYY-ZZZZZZC"), YCodeSpec.of("12", "13", "131", "132", "135", "137"))
        )));

        // 19. 광주은행 (GWANGJU) - 034
        rules.add(new InstitutionRule(BankName.GWANGJU, Confidence.HIGH, List.of(
                new PatternSpec(List.of("YYY-ZZ-ZZZZZZC", "YYY-ZZZ-ZZZZZZC"), YCodeSpec.of("101", "103", "107", "108", "109", "121", "122", "123", "124", "127"))
        )));

        // 20. 전북은행 (JEONBUK) - 037
        rules.add(new InstitutionRule(BankName.JEONBUK, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YY-ZZZZZZC", "YYY-ZZ-ZZZZZZZC"), YCodeSpec.of("01", "02", "03", "11", "12", "13", "15", "21", "22", "23", "35", "36", "37", "011", "012", "013", "021", "023", "501", "502", "513", "522", "538"))
        )));

        // 21. 경남은행 (GYEONGNAM) - 039
        rules.add(new InstitutionRule(BankName.GYEONGNAM, Confidence.HIGH, List.of(
                new PatternSpec(List.of("XXX-YY-ZZZZZZC", "YYY-ZZ-ZZZZZZZC"), YCodeSpec.of("01", "03", "07", "09", "21", "22", "35", "201", "203", "207", "209", "221", "222", "235"))
        )));

        // 22. 제주은행 (JEJU) - 035
        rules.add(new InstitutionRule(BankName.JEJU, Confidence.HIGH, List.of(
                new PatternSpec(List.of("YYY-ZZ-ZZZZZZC", "YYY-ZZ-ZZZZZC"), YCodeSpec.of("010", "020", "030", "040", "050", "700", "707", "711", "712", "713", "714", "769", "770"))
        )));

        return rules;
    }
}
