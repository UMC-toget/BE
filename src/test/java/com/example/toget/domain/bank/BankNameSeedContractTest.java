package com.example.toget.domain.bank;

import com.example.toget.global.enums.BankName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BankName enum이 banks 테이블 스키마 제약을 만족하는지 검증한다.
 *
 * <p>[왜 필요한가] banks의 행은 BankSeeder가 enum을 그대로 옮겨 담아 만든다.
 * 따라서 enum에 상수를 추가할 때 표시명이 너무 길거나 비어 있으면,
 * 컴파일은 통과하고 서버 기동 시점에 INSERT가 깨진다.
 * 그 실패를 배포 전에 잡기 위한 계약 테스트다.
 *
 * <p>여기의 상수는 Bank 엔티티의 @Column 정의와 짝이므로, 한쪽을 바꾸면 다른 쪽도 함께 봐야 한다.
 */
class BankNameSeedContractTest {

    /** Bank.code 컬럼 길이 (@Column(name = "code", length = 30)) */
    private static final int CODE_MAX_LENGTH = 30;
    /** Bank.displayName 컬럼 길이 (@Column(name = "display_name", length = 50)) */
    private static final int DISPLAY_NAME_MAX_LENGTH = 50;

    @Test
    @DisplayName("모든 은행 코드가 code 컬럼 길이(30자) 안에 들어간다")
    void codeFitsColumnLength() {
        assertThat(BankName.values())
                .allSatisfy(bank -> assertThat(bank.name()).hasSizeLessThanOrEqualTo(CODE_MAX_LENGTH));
    }

    @Test
    @DisplayName("모든 은행 표시명이 비어 있지 않고 display_name 컬럼 길이(50자) 안에 들어간다")
    void displayNameIsPresentAndFitsColumnLength() {
        assertThat(BankName.values()).allSatisfy(bank -> {
            assertThat(bank.getDisplayName())
                    .as("%s의 표시명", bank.name())
                    .isNotBlank();
            assertThat(bank.getDisplayName())
                    .as("%s의 표시명 길이", bank.name())
                    .hasSizeLessThanOrEqualTo(DISPLAY_NAME_MAX_LENGTH);
        });
    }

    @Test
    @DisplayName("표시명이 중복되지 않는다 — 사용자가 바텀시트에서 같은 이름 두 개를 보면 구분할 수 없다")
    void displayNamesAreUnique() {
        assertThat(Arrays.stream(BankName.values()).map(BankName::getDisplayName).distinct().count())
                .isEqualTo(BankName.values().length);
    }
}
