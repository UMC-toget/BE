package com.example.toget.global.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 대용량 부하 테스트용 더미 데이터 무작위 생성 유틸리티.
 * 다중 조합(Combinatorial Combination) 및 동적 음절 생성 알고리즘을 도입하여
 * 백만 건 주입 시 문구, 이름, 제목, 상품명 등이 거의 다시 쓰이지 않도록 다양성을 극대화합니다.
 */
public class MockDataRandomUtil {

    // ==========================================
    // 1. 이름 관련 조합 풀 (성씨 + 음절 조합 => 10만+ 이상 조합)
    // ==========================================
    private static final String[] SURNAMES = {
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
            "한", "오", "서", "신", "권", "황", "안", "송", "류", "홍",
            "고", "문", "양", "손", "배", "조", "백", "허", "유", "남",
            "심", "노", "하", "곽", "성", "차", "주", "우", "구", "신",
            "임", "나", "전", "민", "유", "진", "지", "엄", "채", "원",
            "천", "방", "공", "강", "현", "함", "변", "염", "석", "선",
            "남궁", "황보", "제갈", "독고", "선우", "사공"
    };

    private static final String[] FIRST_SYLLABLES = {
            "민", "서", "도", "예", "시", "하", "지", "주", "준", "성",
            "재", "우", "건", "현", "승", "정", "윤", "채", "은", "소",
            "아", "유", "리", "세", "가", "보", "태", "효", "진", "영",
            "훈", "율", "다", "희", "린", "연", "경", "미", "혜", "나",
            "솔", "로", "루", "안", "누", "솜", "봄", "단", "솔", "별"
    };

    private static final String[] SECOND_SYLLABLES = {
            "준", "우", "윤", "현", "우", "재", "진", "우", "원", "호",
            "연", "현", "은", "윤", "서", "유", "민", "원", "아", "율",
            "하", "나", "은", "원", "진", "원", "린", "아", "희", "겸",
            "경", "혁", "훈", "영", "성", "태", "하", "빈", "안", "주"
    };

    // ==========================================
    // 2. 닉네임 다중 수식어/명사 조합 풀 (수천만 가지 이상 조합)
    // ==========================================
    private static final String[] ADV_MODIFIERS = {
            "매우", "유달리", "더욱", "언제나", "오늘따라", "세상에서 제일", "우주급으로", "자타공인", "자발적", "수줍게",
            "소소하게", "갓생사는", "도파민 팡팡", "은은하게", "포근하게", "산뜻하게", "화사하게", "칠전팔기", "몽글몽글", "초롱초롱"
    };

    private static final String[] ADJECTIVES = {
            "행복한", "신나는", "멋진", "빛나는", "달콤한", "푸른", "꿈꾸는", "해피한", "도도한", "귀여운",
            "따뜻한", "상큼한", "차분한", "날렵한", "용감한", "기분좋은", "은은한", "화사한", "포근한", "산뜻한",
            "엉뚱한", "슬기로운", "단아한", "씩씩한", "반짝이는", "소중한", "감성적인", "자유로운", "청량한", "맑은",
            "눈부신", "찬란한", "스윗한", "몽환적인", "낭만적인", "설레는", "싱그러운", "풋풋한", "유쾌한", "따스한"
    };

    private static final String[] NOUN_PREFIXES = {
            "아침", "새벽", "노을", "밤하늘", "봄날의", "여름날의", "가을빛", "겨울눈", "숲속", "바닷가",
            "달빛아래", "별빛속", "구름위", "무지개너머", "은하수", "바람결", "호숫가", "비밀의", "해질녘", "첫눈"
    };

    private static final String[] NOUNS = {
            "토끼", "곰돌이", "고양이", "강아지", "호랑이", "다람쥐", "사슴", "펭귄", "파랑새", "딸기",
            "초코", "바닐라", "라떼", "아메리카노", "구름", "무지개", "별빛", "달빛", "햇살", "단풍",
            "은하수", "바람", "파도", "바다", "하늘", "피치", "자몽", "멜론", "민트", "올리브",
            "판다", "햄스터", "쿼카", "레서판다", "에스프레소", "민트초코", "소금빵", "카스테라", "마카롱", "휘낭시에",
            "우주선", "오로라", "모닥불", "아침이슬", "도토리", "알파카", "수달", "망고", "자두", "복숭아"
    };

    private static final String[] HANDLE_SUFFIXES = {
            "_official", "_vlog", "_daily", "_life", "_lover", "_holic", "_story", "_room", "_log", "_studio",
            "_archive", "_lab", "_diary", "_world", "_zone", "_note", "_gram", "_space", "_club", "_picker"
    };

    private static final String[] EMAIL_DOMAINS = {
            "gmail.com", "naver.com", "daum.net", "kakao.com", "hanmail.net", "icloud.com", "outlook.com", "yahoo.com", "tistory.com", "proton.me"
    };

    // ==========================================
    // 3. 상품명 및 브랜드 다층 구조 조합 풀
    // ==========================================
    private static final String[] BRANDS = {
            "조말론", "딥티크", "이솝", "바이레도", "르라보", "산타마리아노벨라", "크리드", "메종마르지엘라", "샤넬", "디올",
            "톰포드", "로에베", "논픽션", "템버린즈", "아쿠아디파르마", "불리", "입생로랑", "나스", "맥", "헤라",
            "애플", "삼성", "다이슨", "발뮤다", "네스프레소", "드롱기", "루메나", "마샬", "보스", "젠하이저",
            "젠틀몬스터", "아미", "메종키츠네", "마르디메크르디", "마르지엘라", "아크네스튜디오", "우영미", "커버낫", "폴로랄프로렌",
            "무인양품", "자주", "자라홈", "이케아", "카카오프렌즈", "정관장", "오설록", "스타벅스", "블루보틀", "TWG"
    };

    private static final String[] ITEM_PREFIXES_PART1 = {
            "[시그니처]", "[한정판]", "[기프트 에디션]", "[베스트셀러]", "[스페셜 팩]", "[프리미엄 Set]",
            "[홀리데이 컬렉션]", "[데일리 에센셜]", "[선물 포장 포함]", "[초특가]", "[1+1 기획]", "[카카오 단독]",
            "[각인 서비스 포함]", "[시즌 한정]", "[프리미엄 럭셔리]", "[NEW 신상품]", "[NEW 리뉴얼]", "[단독 구성]"
    };

    private static final String[] ITEM_PREFIXES_PART2 = {
            "유기농", "프리미엄", "딥 리프레싱", "인텐시브 모이스처", "올데이 카밍", "스페셜 케어", "하이드레이팅", "어드밴스드",
            "소프트 앤 젠틀", "비건 포뮬러", "헤리티지 리미티드", "아로마테라피", "글로우 리바이탈", "울트라 리치", "센시티브 릴리프"
    };

    private static final String[] ITEM_NAMES_PART1 = {
            "핸드크림", "바디워시", "오 드 파르펭", "아로마 디퓨저", "텀블러", "립밤 & 바디밤", "수제 쿠키", "원두 3종",
            "캔들 워머", "미니 가습기", "노이즈캔슬링 이어폰", "뷰티 가전", "원목 무드등", "세면 타월", "와인 오프너",
            "비타민 멀티팩", "홍삼 정옥고", "숙면 안대", "캠핑 LED 램프", "테이블 오르골", "룸 스프레이", "가죽 카드지갑",
            "블루투스 스피커", "티 세트 컬렉션", "헤어 오일", "워치 스트랩", "오일 롤온", "수제 초콜릿", "드립백 기프트", "보습 에센스"
    };

    private static final String[] ITEM_SPECS = {
            "50ml", "75ml", "100ml", "200ml", "500ml", "30포", "5종 세트", "16구 Box", "44mm", "미니 사이즈",
            "(각인포함)", "(Gift Bag 포함)", "(리필형)", "(2026 스페셜 패키지)", "(무료 배송)", "(선물용 리본 포장)"
    };

    private static final String[] SHOP_DOMAINS = {
            "kakaogift.kakao.com/product/",
            "brand.naver.com/store/products/",
            "www.coupang.com/vp/products/",
            "www.29cm.co.kr/product/",
            "www.musinsa.com/app/goods/",
            "www.wconcept.co.kr/Product/",
            "www.oliveyoung.co.kr/store/goods/getGoodsDetail.do?goodsNo=",
            "www.ssg.com/item/itemView.ssg?itemId=",
            "www.kurly.com/goods/",
            "www.lotteon.com/p/product/"
    };

    private static final String[] SAMPLE_IMAGES = {
            "https://images.unsplash.com/photo-1512496015851-a90fb38ba796",
            "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e",
            "https://images.unsplash.com/photo-1541643600914-78b084683601",
            "https://images.unsplash.com/photo-1523293182086-7651a899d37f",
            "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f",
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e",
            "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae",
            "https://images.unsplash.com/photo-1563178406-4cdc2923acbc",
            "https://images.unsplash.com/photo-1544816155-12df9643f363",
            "https://images.unsplash.com/photo-1572635196237-14b3f281503f"
    };

    // ==========================================
    // 4. 펀딩 제목 및 메시지 다단계 조합 풀 (백만 건 이상 고유 문장 생성)
    // ==========================================
    private static final String[] FUNDING_RELATIONS = {
            "소중한 동기", "아끼는 후배", "존경하는", "우리 그룹", "친애하는", "영원한 친구", "하나뿐인", "든든한 선배", "사랑하는"
    };

    private static final String[] INDIVIDUAL_OCCASIONS = {
            "생일을 진심으로 축하해 주세요! 🎂",
            "생일을 기념하는 축하 펀딩 🎁",
            "첫 자취 및 입주 축하 선물함 🏠",
            "영광스러운 대학 졸업 축하 🎓",
            "꿈꾸던 취업 성공 기념 모금 🎉",
            "무사 전역을 축하하며 같이 준비해요!",
            "이직 및 승진 축하 마음 나누기 ✨",
            "결심! 꿈의 선물 도전 프로젝트 🚀",
            "석사 학위 취득 축하 프로젝트 📜",
            "새로운 시작 축하 서프라이즈 💐"
    };

    private static final String[] TOGETHER_OCCASIONS = {
            "10주년 장기 근속 서프라이즈 선물 🎁",
            "생일 선물 친구들과 함께 준비하기!",
            "결혼 축하 서프라이즈 선물 모금 💍",
            "은퇴 기념 마음 모은 특별 선물 💐",
            "신혼집 집들이 가전 함께 정하기 📺",
            "승진 축하 펀딩 프로젝트 👏",
            "30대 시작 축하 생일 선물 프로젝트 🎉",
            "스승의 날 감사 마음 모으기 💖",
            "퇴사 기념 굿바이 기프트 💌",
            "첫 책 출간 기념 축하 펀딩 📚"
    };

    private static final String[] GREETING_PART1 = {
            "소중한 마음을 담아", "진심으로 축하하는 마음으로", "언제나 함께하는 마음으로", "기쁜 날을 맞이하여", "앞날을 응원하며"
    };

    private static final String[] GREETING_PART2 = {
            "축하해요! 늘 응원합니다 💖",
            "원하던 선물 꼭 받길 바랄게요 ✨",
            "생일 정말 축하해요! 맛있는 거 많이 드세요 🎉",
            "새로운 출발을 축하합니다. 언제나 화이팅!",
            "작은 정성을 보냅니다. 행복 가득한 하루 되세요 😊",
            "앞으로 꽃길만 걸으세요 🌸",
            "축하드립니다! 마음에 들었으면 좋겠네요 🎁",
            "항상 고맙고 사랑합니다! 파이팅 🚀",
            "소중한 추억 많이 만드세요! 적극 응원해요 👍"
    };

    public static Random getRandom() {
        return ThreadLocalRandom.current();
    }

    /** 한국어 성+이름 무작위 생성 (약 10만+ 조합 가능) */
    public static String generateName() {
        Random r = getRandom();
        String surname = SURNAMES[r.nextInt(SURNAMES.length)];
        String first = FIRST_SYLLABLES[r.nextInt(FIRST_SYLLABLES.length)];
        String second = SECOND_SYLLABLES[r.nextInt(SECOND_SYLLABLES.length)];
        return surname + first + second;
    }

    /** 100만 건 전용 조합형 고유 닉네임 (중복 원천 방지) */
    public static String generateUniqueNickname(long index) {
        Random r = getRandom();
        String mod = r.nextBoolean() ? ADV_MODIFIERS[r.nextInt(ADV_MODIFIERS.length)] + " " : "";
        String adj = ADJECTIVES[r.nextInt(ADJECTIVES.length)];
        String nounPre = r.nextBoolean() ? NOUN_PREFIXES[r.nextInt(NOUN_PREFIXES.length)] : "";
        String noun = NOUNS[r.nextInt(NOUNS.length)];

        String baseNick = (mod + adj + nounPre + noun).trim().replaceAll(" ", "_");
        String uniqueTag = Long.toHexString(index ^ 0x7E3A9B2DL).toLowerCase();
        return baseNick + "_" + uniqueTag;
    }

    /** 100만 건 전용 고유 이메일 */
    public static String generateUniqueEmail(long index) {
        Random r = getRandom();
        String domain = EMAIL_DOMAINS[r.nextInt(EMAIL_DOMAINS.length)];
        String uniqueTag = Long.toHexString(index ^ 0x1A2B3C4DL).toLowerCase();
        return "toget_user" + index + "_" + uniqueTag + "@" + domain;
    }

    public static String generateBrand() {
        return BRANDS[getRandom().nextInt(BRANDS.length)];
    }

    /** 10만 건 전용 다층 조합형 고유 상품명 */
    public static String generateUniqueProductName(long index) {
        Random r = getRandom();
        String tag1 = ITEM_PREFIXES_PART1[r.nextInt(ITEM_PREFIXES_PART1.length)];
        String tag2 = r.nextBoolean() ? ITEM_PREFIXES_PART2[r.nextInt(ITEM_PREFIXES_PART2.length)] + " " : "";
        String brand = generateBrand();
        String item = ITEM_NAMES_PART1[r.nextInt(ITEM_NAMES_PART1.length)];
        String spec = ITEM_SPECS[r.nextInt(ITEM_SPECS.length)];
        String sku = String.format("#SKU-%06X", index);

        return tag1 + " " + tag2 + brand + " " + item + " " + spec + " " + sku;
    }

    public static long generateProductPrice() {
        Random r = getRandom();
        int tier = r.nextInt(100);
        if (tier < 15) {
            return (r.nextInt(5) + 5) * 1000L;
        } else if (tier < 45) {
            return (r.nextInt(20) + 10) * 1000L;
        } else if (tier < 75) {
            return (r.nextInt(30) + 30) * 1000L;
        } else if (tier < 90) {
            return (r.nextInt(40) + 60) * 1000L;
        } else {
            return (r.nextInt(25) + 10) * 10000L;
        }
    }

    /** 고유 구매 URL 생성 */
    public static String generateUniquePurchaseUrl(long index) {
        Random r = getRandom();
        String domain = SHOP_DOMAINS[r.nextInt(SHOP_DOMAINS.length)];
        return "https://" + domain + (10000000L + index) + "?ref=toget_mock";
    }

    /** 고유 시드 이미지 URL 생성 */
    public static String generateUniqueImageUrl(long index) {
        return SAMPLE_IMAGES[(int) (index % SAMPLE_IMAGES.length)] + "?auto=format&fit=crop&w=600&q=80&seed=" + index;
    }

    /** 50만 건 전용 조합형 고유 내 선물 펀딩 제목 */
    public static String generateUniqueIndividualFundingTitle(String recipientName, long index) {
        Random r = getRandom();
        String rel = r.nextBoolean() ? FUNDING_RELATIONS[r.nextInt(FUNDING_RELATIONS.length)] + " " : "";
        String occ = INDIVIDUAL_OCCASIONS[r.nextInt(INDIVIDUAL_OCCASIONS.length)];
        return rel + recipientName + "님의 " + occ + " [ID:" + index + "]";
    }

    /** 50만 건 전용 조합형 고유 함께 선물 펀딩 제목 */
    public static String generateUniqueTogetherFundingTitle(String recipientName, long index) {
        Random r = getRandom();
        String rel = r.nextBoolean() ? FUNDING_RELATIONS[r.nextInt(FUNDING_RELATIONS.length)] + " " : "";
        String occ = TOGETHER_OCCASIONS[r.nextInt(TOGETHER_OCCASIONS.length)];
        return rel + recipientName + " " + occ + " [Project#" + index + "]";
    }

    /** 백만 건 전용 조합형 응원 메시지 */
    public static String generateCheeringMessage() {
        Random r = getRandom();
        String p1 = GREETING_PART1[r.nextInt(GREETING_PART1.length)];
        String p2 = GREETING_PART2[r.nextInt(GREETING_PART2.length)];
        return p1 + " " + p2;
    }

    /** 고유 계좌번호 생성 */
    public static String generateUniqueAccountNo(long index, String bankName) {
        long code = 100000000000L + index;
        String str = String.valueOf(code);
        return str.substring(0, 3) + "-" + str.substring(3, 7) + "-" + str.substring(7, 12);
    }

    public static LocalDateTime getRandomDateTimeBetween(int startDaysAgo, int endDaysAgo) {
        Random r = getRandom();
        int days = startDaysAgo + r.nextInt(Math.max(1, endDaysAgo - startDaysAgo));
        int hours = r.nextInt(24);
        int minutes = r.nextInt(60);
        return LocalDateTime.now().minusDays(days).plusHours(hours).plusMinutes(minutes);
    }

    public static LocalDate getRandomDate(int startDaysAgo, int futureDays) {
        Random r = getRandom();
        int diff = r.nextInt(startDaysAgo + futureDays + 1) - startDaysAgo;
        return LocalDate.now().plusDays(diff);
    }
}
