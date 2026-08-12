package com.example.toget.domain.invitation.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvitationBackground 엔티티")
public class InvitationBackgroundTest {

    @Nested
    @DisplayName("생성 시")
    public class Create {

        @Test
        @DisplayName("이름, HEX 코드, 원색 HEX 코드가 유효하면 정상 생성된다")
        void create_success() {
            InvitationBackground background = InvitationBackground.builder()
                    .name("파스텔 옐로우")
                    .hexCode("#FFFFE0")
                    .solidColorHex("#FFD700")
                    .build();

            assertThat(background.getName()).isEqualTo("파스텔 옐로우");
            assertThat(background.getHexCode()).isEqualTo("#FFFFE0");
            assertThat(background.getSolidColorHex()).isEqualTo("#FFD700");
        }
    }

    @Nested
    @DisplayName("수정 시")
    public class Update {

        @Test
        @DisplayName("유효한 값으로 수정하면 필드가 갱신된다")
        void update_success() {
            InvitationBackground background = InvitationBackground.builder()
                    .name("파스텔 옐로우")
                    .hexCode("#FFFFE0")
                    .solidColorHex("#FFD700")
                    .build();

            background.update("스카이 블루", "#E0F7FA", "#00ACC1");

            assertThat(background.getName()).isEqualTo("스카이 블루");
            assertThat(background.getHexCode()).isEqualTo("#E0F7FA");
            assertThat(background.getSolidColorHex()).isEqualTo("#00ACC1");
        }
    }
}
