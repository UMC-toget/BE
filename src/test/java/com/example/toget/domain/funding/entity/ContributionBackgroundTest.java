package com.example.toget.domain.funding.entity;

import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContributionBackground 엔티티")
public class ContributionBackgroundTest {

    @Nested
    @DisplayName("생성 시")
    class Create {

        @Test
        @DisplayName("이름과 HEX 코드가 유효하면 정상 생성된다")
        void create_success() {
            // when
            ContributionBackground background = ContributionBackground.create("파스텔 핑크", "#FFB6C1", "#FF007F");

            // then
            assertThat(background.getName()).isEqualTo("파스텔 핑크");
            assertThat(background.getHexCode()).isEqualTo("#FFB6C1");
            assertThat(background.getSolidColorHex()).isEqualTo("#FF007F");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("이름이 null이거나 공백이면 예외가 발생한다")
        void create_fail_whenNameIsBlank(String invalidName) {
            assertThatThrownBy(() -> ContributionBackground.create(invalidName, "#FFB6C1", "#FF007F"))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(ContributionErrorCode.INVALID_BACKGROUND_NAME);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "FFB6C1",      // # 없음
                "#FFB6C",      // 5자리
                "#FFB6C12",    // 7자리
                "#GGGGGG",     // 유효하지 않은 hex 문자
                "#fff",        // 3자리 축약형 (요구사항상 6자리만 허용)
        })
        @DisplayName("HEX 코드 형식이 올바르지 않으면 예외가 발생한다")
        void create_fail_whenHexCodeInvalid(String invalidHexCode) {
            assertThatThrownBy(() -> ContributionBackground.create("파스텔 핑크", invalidHexCode, "#FF007F"))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(ContributionErrorCode.INVALID_HEX_CODE);
        }

        @Test
        @DisplayName("HEX 코드가 null이면 예외가 발생한다")
        void create_fail_whenHexCodeIsNull() {
            assertThatThrownBy(() -> ContributionBackground.create("파스텔 핑크", null, "#FF007F"))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(ContributionErrorCode.INVALID_HEX_CODE);
        }

        @Test
        @DisplayName("원색 HEX 코드가 null이면 예외가 발생한다")
        void create_fail_whenSolidColorHexIsNull() {
            assertThatThrownBy(() -> ContributionBackground.create("파스텔 핑크", "#FFB6C1", null))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(ContributionErrorCode.INVALID_HEX_CODE);
        }

        @Test
        @DisplayName("소문자 hex 코드도 정상 생성된다")
        void create_success_withLowerCaseHex() {
            ContributionBackground background = ContributionBackground.create("파스텔 핑크", "#ffb6c1", "#ff007f");

            assertThat(background.getHexCode()).isEqualTo("#ffb6c1");
            assertThat(background.getSolidColorHex()).isEqualTo("#ff007f");
        }
    }

    @Nested
    @DisplayName("수정 시")
    class Update {

        @Test
        @DisplayName("유효한 값으로 수정하면 필드가 갱신된다")
        void update_success() {
            // given
            ContributionBackground background = ContributionBackground.create("파스텔 핑크", "#FFB6C1", "#FF007F");

            // when
            background.update("핫 핑크", "#FF69B4", "#FF0055");

            // then
            assertThat(background.getName()).isEqualTo("핫 핑크");
            assertThat(background.getHexCode()).isEqualTo("#FF69B4");
            assertThat(background.getSolidColorHex()).isEqualTo("#FF0055");
        }

        @Test
        @DisplayName("잘못된 값으로 수정하면 예외가 발생하고 기존 값은 유지된다")
        void update_fail_keepsOriginalValue() {
            // given
            ContributionBackground background = ContributionBackground.create("파스텔 핑크", "#FFB6C1", "#FF007F");

            // when & then
            assertThatThrownBy(() -> background.update("", "#FF69B4", "#FF0055"))
                    .isInstanceOf(ProjectException.class);

            // 예외 발생 후에도 기존 값이 그대로인지 확인 (부분 갱신 방지)
            assertThat(background.getName()).isEqualTo("파스텔 핑크");
            assertThat(background.getHexCode()).isEqualTo("#FFB6C1");
            assertThat(background.getSolidColorHex()).isEqualTo("#FF007F");
        }
    }
}