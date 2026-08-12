package com.example.toget.global.mock;

import com.example.toget.global.util.AesGcmConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 부하 테스트용 대량 더미 데이터 Batch Insert 서비스.
 * 중복 없는 고유(Unique) 데이터 및 높은 다양성을 보장하며 100만 건 규모의 데이터를 고속 주입합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataBatchService {

    private final JdbcTemplate jdbcTemplate;
    private final AesGcmConverter aesGcmConverter;

    private static final int BATCH_SIZE = 5000;

    @Transactional
    public void generateAllMockData(double scale) {
        long startTime = System.currentTimeMillis();
        log.info("============== [고유 목 데이터 대량 주입 시작] (Scale: {}x) ==============", scale);

        // 1. 참조용 고정 마스터 데이터 ID 조회 (캐릭터, 배경색, 은행)
        List<Long> characterIds = fetchIds("SELECT id FROM characters");
        List<Long> invitationBgIds = fetchIds("SELECT id FROM invitation_backgrounds");
        List<Long> contributionBgIds = fetchIds("SELECT id FROM contribution_backgrounds");
        List<Long> bankIds = fetchIds("SELECT bank_id FROM banks");

        log.info("마스터 데이터 참조 상태: characters={}(개), invitation_backgrounds={}(개), contribution_backgrounds={}(개), banks={}(개)",
                characterIds.size(), invitationBgIds.size(), contributionBgIds.size(), bankIds.size());

        // 2. 회원 (users) - 목표 500,000건 * scale
        int userCount = (int) (500_000 * scale);
        log.info("1/19. users 테이블 고유 데이터 주입 중... (목표: {}건)", userCount);
        insertUsers(userCount);
        List<Long> userIds = fetchIds("SELECT user_id FROM users ORDER BY user_id DESC LIMIT " + userCount);

        // 3. 사용자 계좌 (user_accounts) - 목표 400,000건 * scale
        int accountCount = (int) (400_000 * scale);
        log.info("2/19. user_accounts 테이블 데이터 주입 중... (목표: {}건)", accountCount);
        insertUserAccounts(accountCount, userIds, bankIds);
        List<Long> accountIds = fetchIds("SELECT user_account_id FROM user_accounts ORDER BY user_account_id DESC LIMIT " + accountCount);

        // 4. 상품 (products) & 상품 카테고리 (product_category) - 목표 100,000건 * scale
        int productCount = (int) (100_000 * scale);
        log.info("3/19. products & product_category 테이블 고유 데이터 주입 중... (목표: {}건)", productCount);
        insertProductsAndCategories(productCount);
        List<Long> productIds = fetchIds("SELECT product_id FROM products ORDER BY product_id DESC LIMIT " + productCount);

        // 5. 위시리스트 (wishlist_items) - 목표 1,000,000건 * scale
        int wishlistCount = (int) (1_000_000 * scale);
        log.info("4/19. wishlist_items 테이블 데이터 주입 중... (목표: {}건)", wishlistCount);
        insertWishlistItems(wishlistCount, userIds, productIds);

        // 6. 펀딩 메인 (fundings) - 목표 500,000건 * scale (MY_GIFT 60%, TOGETHER_GIFT 40%)
        int fundingCount = (int) (500_000 * scale);
        log.info("5/19. fundings 테이블 고유 데이터 주입 중... (목표: {}건)", fundingCount);
        insertFundings(fundingCount, userIds, accountIds);

        List<Long> myGiftFundingIds = fetchIds("SELECT id FROM fundings WHERE funding_type = 'MY_GIFT' ORDER BY id DESC LIMIT " + fundingCount);
        List<Long> togetherFundingIds = fetchIds("SELECT id FROM fundings WHERE funding_type = 'TOGETHER_GIFT' ORDER BY id DESC LIMIT " + fundingCount);
        List<Long> allFundingIds = new ArrayList<>(myGiftFundingIds);
        allFundingIds.addAll(togetherFundingIds);

        // 7. 초대장 카드 (invitation_cards) - 1:1 매핑 (500,000건)
        log.info("6/19. invitation_cards 테이블 데이터 주입 중... (목표: {}건)", allFundingIds.size());
        insertInvitationCards(allFundingIds, characterIds, invitationBgIds);

        // 8. 펀딩 노출 설정 (funding_visibility_settings) - 1:1 매핑 (MY_GIFT 대상)
        log.info("7/19. funding_visibility_settings 테이블 데이터 주입 중... (목표: {}건)", myGiftFundingIds.size());
        insertVisibilitySettings(myGiftFundingIds);

        // 9. 함께 선물 참여자 (funding_members) - TOGETHER_GIFT 당 3~15명 (목표 1,000,000건)
        log.info("8/19. funding_members 테이블 데이터 주입 중...");
        insertFundingMembers(togetherFundingIds, userIds);

        // 10. 등록 선물 / 후보 선물 (funding_gifts) - 목표 1,000,000건
        log.info("9/19. funding_gifts 테이블 데이터 주입 중...");
        insertFundingGifts(myGiftFundingIds, togetherFundingIds);
        List<Long> togetherGiftIds = fetchIds("SELECT funding_gift_id FROM funding_gifts WHERE funding_member_id IS NOT NULL ORDER BY funding_gift_id DESC LIMIT 500000");

        // 11. 펀딩 금액 참여 / 입금 내역 (funding_contributions) - 목표 1,000,000건
        log.info("10/19. funding_contributions 테이블 데이터 주입 중...");
        insertFundingContributions((int) (1_000_000 * scale), allFundingIds, userIds, contributionBgIds);

        // 12. 후보 선물 투표 (funding_gift_votes) - 목표 800,000건
        log.info("11/19. funding_gift_votes 테이블 데이터 주입 중...");
        insertGiftVotes((int) (800_000 * scale), togetherGiftIds);

        // 13. 후보 선물 댓글 (funding_gift_comments) - 목표 500,000건
        log.info("12/19. funding_gift_comments 테이블 데이터 주입 중...");
        insertGiftComments((int) (500_000 * scale), togetherGiftIds);

        // 14. 최종 구매 확정 (funding_gift_purchases) - 목표 150,000건
        log.info("13/19. funding_gift_purchases 테이블 데이터 주입 중...");
        insertGiftPurchases((int) (150_000 * scale), togetherGiftIds);

        // 15. 선물 후기 (funding_reviews) - 목표 200,000건
        log.info("14/19. funding_reviews 테이블 데이터 주입 중...");
        insertFundingReviews((int) (200_000 * scale), allFundingIds, contributionBgIds, characterIds, invitationBgIds);
        List<Long> reviewIds = fetchIds("SELECT id FROM funding_reviews ORDER BY id DESC LIMIT " + (int) (200_000 * scale));

        // 16. 선물 후기 이미지 (funding_review_images) - 목표 300,000건
        log.info("15/19. funding_review_images 테이블 데이터 주입 중...");
        insertReviewImages((int) (300_000 * scale), reviewIds);

        // 17~19. 임시저장 (drafts 3종) - 목표 180,000건
        log.info("16/19 ~ 19/19. drafts 테이블 데이터 주입 중...");
        insertDrafts((int) (50_000 * scale), userIds, accountIds, characterIds, invitationBgIds);

        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        log.info("============== [고유 목 데이터 주입 완료! 소요 시간: {}초] ==============", elapsedTime);
    }

    private List<Long> fetchIds(String query) {
        try {
            return jdbcTemplate.queryForList(query, Long.class);
        } catch (Exception e) {
            log.warn("아이디 조회 중 쿼리 오류 발생: {}. 기본 값 [1] 반환.", e.getMessage());
            return List.of(1L);
        }
    }

    private void insertUsers(int count) {
        String sql = "INSERT INTO users (oauth_provider, oauth_id, email, name, nickname, profile_image_url, status, refresh_token, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();

        for (int i = 1; i <= count; i++) {
            boolean isActive = r.nextDouble() < 0.90;
            String provider = r.nextDouble() < 0.65 ? "KAKAO" : "GOOGLE";
            String oauthId = isActive ? provider.toLowerCase() + "_" + (10000000 + i) : "withdrawn:" + UUID.randomUUID();
            String email = isActive ? MockDataRandomUtil.generateUniqueEmail(i) : null;
            String name = isActive ? MockDataRandomUtil.generateName() : null;
            String nickname = isActive ? MockDataRandomUtil.generateUniqueNickname(i) : null;
            String profileImg = (isActive && r.nextBoolean()) ? MockDataRandomUtil.generateUniqueImageUrl(i) : null;
            String status = isActive ? "ACTIVE" : "WITHDRAWN";
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(300, 1);

            batch.add(new Object[]{
                    provider, oauthId, email, name, nickname, profileImg, status, null,
                    Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
            });

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertUserAccounts(int count, List<Long> userIds, List<Long> bankIds) {
        if (userIds.isEmpty()) return;
        String sql = "INSERT INTO user_accounts (user_id, bank_name, bank_id, account_owner, account, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        com.example.toget.global.enums.BankName[] allBankEnums = com.example.toget.global.enums.BankName.values();
        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();

        for (int i = 0; i < count; i++) {
            Long userId = userIds.get(i % userIds.size());
            com.example.toget.global.enums.BankName bankEnum = allBankEnums[r.nextInt(allBankEnums.length)];
            String bankName = bankEnum.name();
            Long bankId = bankIds.isEmpty() ? null : bankIds.get(r.nextInt(bankIds.size()));
            String owner = MockDataRandomUtil.generateName();
            String rawAccount = MockDataRandomUtil.generateUniqueAccountNo(i + 1, bankName);
            String encryptedAccount;
            try {
                encryptedAccount = aesGcmConverter.convertToDatabaseColumn(rawAccount);
            } catch (Exception e) {
                encryptedAccount = rawAccount;
            }

            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(250, 1);

            batch.add(new Object[]{
                    userId, bankName, bankId, owner, encryptedAccount,
                    Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
            });

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertProductsAndCategories(int count) {
        String productSql = "INSERT INTO products (name, price, description, image_url, shop_url, brand, wishlist_count, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String categorySql = "INSERT INTO product_category (product_id, category_type) VALUES (?, ?)";

        List<Object[]> productBatch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        String[] categories = {"BIRTHDAY", "GRADUATION", "HOUSEWARMING"};

        for (int i = 1; i <= count; i++) {
            String name = MockDataRandomUtil.generateUniqueProductName(i);
            long price = MockDataRandomUtil.generateProductPrice();
            String brand = MockDataRandomUtil.generateBrand();
            String shopUrl = MockDataRandomUtil.generateUniquePurchaseUrl(i);
            String imgUrl = MockDataRandomUtil.generateUniqueImageUrl(i);
            long wishlistCount = r.nextInt(500);
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(365, 10);

            productBatch.add(new Object[]{
                    name, price, "부하 테스트용 큐레이션 상품 고유 정보입니다. (" + i + ")", imgUrl, shopUrl, brand, wishlistCount,
                    Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
            });

            if (productBatch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(productSql, productBatch);
                productBatch.clear();
            }
        }
        if (!productBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(productSql, productBatch);
        }

        List<Long> productIds = fetchIds("SELECT product_id FROM products ORDER BY product_id DESC LIMIT " + count);
        List<Object[]> categoryBatch = new ArrayList<>();

        for (Long productId : productIds) {
            int catCount = r.nextInt(3) + 1;
            Set<String> selectedCats = new HashSet<>();
            for (int k = 0; k < catCount; k++) {
                selectedCats.add(categories[r.nextInt(categories.length)]);
            }
            for (String cat : selectedCats) {
                categoryBatch.add(new Object[]{productId, cat});
            }

            if (categoryBatch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(categorySql, categoryBatch);
                categoryBatch.clear();
            }
        }
        if (!categoryBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(categorySql, categoryBatch);
        }
    }

    private void insertWishlistItems(int count, List<Long> userIds, List<Long> productIds) {
        if (userIds.isEmpty() || productIds.isEmpty()) return;
        String sql = "INSERT INTO wishlist_items (user_id, product_id, name, price, purchase_url, image_url, type, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        String[] wishlistTypes = {"GIVE", "RECEIVE"};

        for (int i = 1; i <= count; i++) {
            Long userId = userIds.get(r.nextInt(userIds.size()));
            Long productId = productIds.get(r.nextInt(productIds.size()));
            String name = MockDataRandomUtil.generateUniqueProductName(100000 + i);
            long price = MockDataRandomUtil.generateProductPrice();
            String shopUrl = MockDataRandomUtil.generateUniquePurchaseUrl(100000 + i);
            String imgUrl = MockDataRandomUtil.generateUniqueImageUrl(100000 + i);
            String type = wishlistTypes[r.nextInt(wishlistTypes.length)];
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(180, 1);

            batch.add(new Object[]{
                    userId, productId, name, price, shopUrl, imgUrl, type,
                    Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
            });

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertFundings(int count, List<Long> userIds, List<Long> accountIds) {
        if (userIds.isEmpty()) return;
        String sql = "INSERT INTO fundings (user_id, user_account_id, funding_type, title, recipient_name, anniversary_date, start_date, end_date, introduction, thumbnail_image_url, target_amount, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        String[] statuses = {"SELECTING", "SETTLING", "PURCHASING", "DELIVERING", "ENDED"};

        for (int i = 1; i <= count; i++) {
            Long userId = userIds.get(r.nextInt(userIds.size()));
            boolean isMyGift = r.nextDouble() < 0.60;
            String type = isMyGift ? "MY_GIFT" : "TOGETHER_GIFT";
            Long accountId = isMyGift ? (accountIds.isEmpty() ? null : accountIds.get(r.nextInt(accountIds.size()))) : (r.nextBoolean() ? null : (accountIds.isEmpty() ? null : accountIds.get(r.nextInt(accountIds.size()))));

            String recipientName = MockDataRandomUtil.generateName();
            String title = isMyGift ? MockDataRandomUtil.generateUniqueIndividualFundingTitle(recipientName, i) : MockDataRandomUtil.generateUniqueTogetherFundingTitle(recipientName, i);

            LocalDate anniversary = MockDataRandomUtil.getRandomDate(10, 30);
            LocalDate startDate = MockDataRandomUtil.getRandomDate(30, 0);
            LocalDate endDate = startDate.plusDays(7 + r.nextInt(20));

            String status = isMyGift ? (r.nextDouble() < 0.70 ? "SETTLING" : "ENDED") : statuses[r.nextInt(statuses.length)];
            long targetAmount = (r.nextInt(30) + 5) * 10000L;
            String imgUrl = MockDataRandomUtil.generateUniqueImageUrl(200000 + i);
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(90, 1);

            batch.add(new Object[]{
                    userId, accountId, type, title, recipientName,
                    java.sql.Date.valueOf(anniversary), java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate),
                    "특별한 선물을 준비하는 고유 펀딩입니다. (#" + i + ")", imgUrl, targetAmount, status,
                    Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
            });

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertInvitationCards(List<Long> fundingIds, List<Long> charIds, List<Long> bgIds) {
        if (fundingIds.isEmpty()) return;
        String sql = "INSERT INTO invitation_cards (funding_id, character_id, background_id, title, content, url) VALUES (?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        long counter = 1;

        for (Long fundingId : fundingIds) {
            Long charId = charIds.isEmpty() ? 1L : charIds.get(r.nextInt(charIds.size()));
            Long bgId = bgIds.isEmpty() ? 1L : bgIds.get(r.nextInt(bgIds.size()));
            String title = "초대합니다! #" + counter;
            String content = MockDataRandomUtil.generateCheeringMessage();
            String url = "https://toget.site/invitation/" + UUID.randomUUID().toString();
            counter++;

            batch.add(new Object[]{fundingId, charId, bgId, title, content, url});

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertVisibilitySettings(List<Long> myGiftFundingIds) {
        if (myGiftFundingIds.isEmpty()) return;
        String sql = "INSERT INTO funding_visibility_settings (funding_id, is_progress_visible, is_participant_count_visible, is_participant_name_visible, is_message_visible, is_collected_amount_visible, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long fundingId : myGiftFundingIds) {
            batch.add(new Object[]{fundingId, true, true, true, true, true, Timestamp.valueOf(now), Timestamp.valueOf(now)});

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertFundingMembers(List<Long> togetherFundingIds, List<Long> userIds) {
        if (togetherFundingIds.isEmpty() || userIds.isEmpty()) return;
        String sql = "INSERT INTO funding_members (funding_id, user_id, role, amount_due, settlement_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        String[] settlementStatuses = {"UNPAID", "PAID", "CONFIRMED"};

        for (Long fundingId : togetherFundingIds) {
            int memberCount = r.nextInt(12) + 3;
            Set<Long> pickedUsers = new HashSet<>();

            for (int k = 0; k < memberCount; k++) {
                Long userId = userIds.get(r.nextInt(userIds.size()));
                if (!pickedUsers.add(userId)) continue;

                String role = (k == 0) ? "CREATOR" : (k == 1 ? "ADMIN" : "PARTICIPANT");
                boolean isSettled = r.nextBoolean();
                Long amountDue = isSettled ? (r.nextInt(5) + 1) * 10000L : null;
                String settlementStatus = isSettled ? settlementStatuses[r.nextInt(settlementStatuses.length)] : null;
                LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(60, 1);

                batch.add(new Object[]{
                        fundingId, userId, role, amountDue, settlementStatus,
                        Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertFundingGifts(List<Long> myGiftIds, List<Long> togetherIds) {
        String sql = "INSERT INTO funding_gifts (funding_id, funding_member_id, gift_name, gift_price, gift_purchase_url, gift_image_url, status, note, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        long giftCounter = 1;

        for (Long fundingId : myGiftIds) {
            int giftCount = r.nextInt(3) + 1;
            for (int k = 0; k < giftCount; k++) {
                String name = MockDataRandomUtil.generateUniqueProductName(300000 + giftCounter);
                long price = MockDataRandomUtil.generateProductPrice();
                String purchaseUrl = MockDataRandomUtil.generateUniquePurchaseUrl(300000 + giftCounter);
                String imgUrl = MockDataRandomUtil.generateUniqueImageUrl(300000 + giftCounter);
                LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(60, 1);
                giftCounter++;

                batch.add(new Object[]{
                        fundingId, null, name, price, purchaseUrl, imgUrl, "SELECTED", null,
                        Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    batch.clear();
                }
            }
        }

        for (Long fundingId : togetherIds) {
            int giftCount = r.nextInt(4) + 2;
            for (int k = 0; k < giftCount; k++) {
                String name = MockDataRandomUtil.generateUniqueProductName(300000 + giftCounter);
                long price = MockDataRandomUtil.generateProductPrice();
                String purchaseUrl = MockDataRandomUtil.generateUniquePurchaseUrl(300000 + giftCounter);
                String imgUrl = MockDataRandomUtil.generateUniqueImageUrl(300000 + giftCounter);
                String status = (k == 0) ? "SELECTED" : "CANDIDATE";
                LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(60, 1);
                giftCounter++;

                batch.add(new Object[]{
                        fundingId, 1L, name, price, purchaseUrl, imgUrl, status, "후보 선물 추천합니다 (" + giftCounter + ")",
                        Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
                });

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertFundingContributions(int count, List<Long> fundingIds, List<Long> userIds, List<Long> bgIds) {
        if (fundingIds.isEmpty()) return;
        String sql = "INSERT INTO funding_contributions (funding_id, background_id, user_id, guest_name, is_anonymous, amount, content, is_message_visible, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();

        for (int i = 1; i <= count; i++) {
            Long fundingId = fundingIds.get(r.nextInt(fundingIds.size()));
            Long bgId = bgIds.isEmpty() ? 1L : bgIds.get(r.nextInt(bgIds.size()));
            boolean isGuest = r.nextBoolean();

            Long userId = isGuest ? null : userIds.get(r.nextInt(userIds.size()));
            String guestName = isGuest ? MockDataRandomUtil.generateName() : null;
            boolean isAnon = r.nextDouble() < 0.20;
            long amount = (r.nextInt(10) + 1) * 10000L;
            String content = MockDataRandomUtil.generateCheeringMessage() + " (#" + i + ")";
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(40, 1);

            batch.add(new Object[]{
                    fundingId, bgId, userId, guestName, isAnon, amount, content, true,
                    Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
            });

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertGiftVotes(int count, List<Long> togetherGiftIds) {
        if (togetherGiftIds.isEmpty()) return;
        String sql = "INSERT INTO funding_gift_votes (funding_gift_id, funding_member_id, created_at, updated_at) VALUES (?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();

        for (int i = 1; i <= count; i++) {
            Long giftId = togetherGiftIds.get(r.nextInt(togetherGiftIds.size()));
            Long memberId = (long) (r.nextInt(100000) + 1);
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(30, 1);

            batch.add(new Object[]{giftId, memberId, Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)});

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertGiftComments(int count, List<Long> togetherGiftIds) {
        if (togetherGiftIds.isEmpty()) return;
        String sql = "INSERT INTO funding_gift_comments (funding_gift_id, funding_member_id, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();

        for (int i = 1; i <= count; i++) {
            Long giftId = togetherGiftIds.get(r.nextInt(togetherGiftIds.size()));
            Long memberId = (long) (r.nextInt(100000) + 1);
            String content = "이 선물 디자인이 제일 예쁜 것 같아요! 추천합니다 👍 (" + i + ")";
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(30, 1);

            batch.add(new Object[]{giftId, memberId, content, Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)});

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertGiftPurchases(int count, List<Long> togetherGiftIds) {
        if (togetherGiftIds.isEmpty()) return;
        String sql = "INSERT INTO funding_gift_purchases (funding_gift_id, purchase_url, receipt_image_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= count && i <= togetherGiftIds.size(); i++) {
            Long giftId = togetherGiftIds.get(i - 1);
            String purchaseUrl = MockDataRandomUtil.generateUniquePurchaseUrl(500000 + i);
            String receiptImg = MockDataRandomUtil.generateUniqueImageUrl(500000 + i);

            batch.add(new Object[]{giftId, purchaseUrl, receiptImg, Timestamp.valueOf(now), Timestamp.valueOf(now)});

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertFundingReviews(int count, List<Long> fundingIds, List<Long> bgIds, List<Long> charIds, List<Long> invBgIds) {
        if (fundingIds.isEmpty()) return;
        String sql = "INSERT INTO funding_reviews (funding_id, type, title, content, background_id, invitation_title, invitation_content, invitation_character_id, invitation_background_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        String[] types = {"REVIEW", "NEWS", "HEARTFELT"};

        for (int i = 1; i <= count && i <= fundingIds.size(); i++) {
            Long fundingId = fundingIds.get(i - 1);
            String type = types[r.nextInt(types.length)];
            String title = "REVIEW".equals(type) ? null : "소중한 마음 감사합니다 🎁 (" + i + ")";
            String content = "축하해 주신 모든 분들 덕분에 행복한 선물을 받았습니다! 감사합니다. #" + i;
            Long bgId = bgIds.isEmpty() ? 1L : bgIds.get(r.nextInt(bgIds.size()));
            Long charId = charIds.isEmpty() ? 1L : charIds.get(r.nextInt(charIds.size()));
            Long invBgId = invBgIds.isEmpty() ? 1L : invBgIds.get(r.nextInt(invBgIds.size()));
            LocalDateTime createdAt = MockDataRandomUtil.getRandomDateTimeBetween(20, 1);

            batch.add(new Object[]{
                    fundingId, type, title, content, bgId, "후기 초대장 #" + i, "후기를 확인하세요!", charId, invBgId,
                    Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
            });

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertReviewImages(int count, List<Long> reviewIds) {
        if (reviewIds.isEmpty()) return;
        String sql = "INSERT INTO funding_review_images (funding_review_id, image_url) VALUES (?, ?)";

        List<Object[]> batch = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            Long reviewId = reviewIds.get((i - 1) % reviewIds.size());
            String imgUrl = MockDataRandomUtil.generateUniqueImageUrl(600000 + i);

            batch.add(new Object[]{reviewId, imgUrl});

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

    private void insertDrafts(int count, List<Long> userIds, List<Long> accountIds, List<Long> charIds, List<Long> bgIds) {
        if (userIds.isEmpty()) return;

        String myDraftSql = "INSERT INTO individual_funding_drafts (user_id, step, title, anniversary_date, start_date, end_date, greeting, thumbnail_url, is_progress_public, is_amount_public, is_participant_count_public, is_participant_name_public, is_message_public, invitation_character_id, invitation_background_id, invitation_title, invitation_content, user_account_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String togetherDraftSql = "INSERT INTO funding_together_drafts (user_id, step, start_date, end_date, title, receiver, anniversary_date, description, thumbnail_image_url, user_account_id, card_title, card_content, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> myDraftBatch = new ArrayList<>();
        List<Object[]> togetherDraftBatch = new ArrayList<>();
        Random r = MockDataRandomUtil.getRandom();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= count; i++) {
            Long userId = userIds.get(r.nextInt(userIds.size()));
            Long accountId = accountIds.isEmpty() ? null : accountIds.get(r.nextInt(accountIds.size()));
            Long charId = charIds.isEmpty() ? 1L : charIds.get(r.nextInt(charIds.size()));
            Long bgId = bgIds.isEmpty() ? 1L : bgIds.get(r.nextInt(bgIds.size()));

            myDraftBatch.add(new Object[]{
                    userId, r.nextInt(4) + 1, "임시 저장 내 선물 펀딩 #" + i,
                    java.sql.Date.valueOf(LocalDate.now().plusDays(10)),
                    java.sql.Date.valueOf(LocalDate.now()),
                    java.sql.Date.valueOf(LocalDate.now().plusDays(14)),
                    "작성 중인 인사말입니다. (" + i + ")", MockDataRandomUtil.generateUniqueImageUrl(700000 + i),
                    true, true, true, true, true, charId, bgId, "임시 초대장 #" + i, "내용", accountId,
                    Timestamp.valueOf(now), Timestamp.valueOf(now)
            });

            togetherDraftBatch.add(new Object[]{
                    userId, r.nextInt(4) + 1,
                    java.sql.Date.valueOf(LocalDate.now()),
                    java.sql.Date.valueOf(LocalDate.now().plusDays(14)),
                    "임시 저장 함께 선물 #" + i, MockDataRandomUtil.generateName(),
                    java.sql.Date.valueOf(LocalDate.now().plusDays(10)),
                    "함께 선물을 준비해 봅시다 (" + i + ")", MockDataRandomUtil.generateUniqueImageUrl(800000 + i), accountId,
                    "카드 제목 #" + i, "카드 내용", Timestamp.valueOf(now), Timestamp.valueOf(now)
            });

            if (myDraftBatch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(myDraftSql, myDraftBatch);
                jdbcTemplate.batchUpdate(togetherDraftSql, togetherDraftBatch);
                myDraftBatch.clear();
                togetherDraftBatch.clear();
            }
        }
        if (!myDraftBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(myDraftSql, myDraftBatch);
            jdbcTemplate.batchUpdate(togetherDraftSql, togetherDraftBatch);
        }
    }
}
