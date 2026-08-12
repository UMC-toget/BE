package com.example.toget.domain.invitation.service;

import com.example.toget.domain.invitation.dto.InvitationBackgroundCreateResponse;
import com.example.toget.domain.invitation.dto.InvitationBackgroundRequest;
import com.example.toget.domain.invitation.dto.InvitationBackgroundResponse;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationBackgroundService")
public class InvitationBackgroundServiceTest {

    @Mock
    private InvitationBackgroundRepository invitationBackgroundRepository;

    @InjectMocks
    private InvitationBackgroundService invitationBackgroundService;

    @Nested
    @DisplayName("전체 조회")
    class GetAllBackgrounds {

        @Test
        @DisplayName("데이터가 있으면 solidColorHex를 포함해 응답 목록으로 변환해 반환한다")
        void getAllBackgrounds_success() {
            InvitationBackground background = InvitationBackground.builder()
                    .name("파스텔 옐로우")
                    .hexCode("#FFFFE0")
                    .solidColorHex("#FFD700")
                    .build();
            ReflectionTestUtils.setField(background, "id", 1L);
            given(invitationBackgroundRepository.findAllByDeletedAtIsNull()).willReturn(List.of(background));

            List<InvitationBackgroundResponse> result = invitationBackgroundService.getAllBackgrounds();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("파스텔 옐로우");
            assertThat(result.get(0).hexCode()).isEqualTo("#FFFFE0");
            assertThat(result.get(0).solidColorHex()).isEqualTo("#FFD700");
        }
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 요청으로 초대장 배경을 생성한다")
        void create_success() {
            InvitationBackgroundRequest request = new InvitationBackgroundRequest("파스텔 옐로우", "#FFFFE0", "#FFD700");
            InvitationBackground saved = InvitationBackground.builder()
                    .name("파스텔 옐로우")
                    .hexCode("#FFFFE0")
                    .solidColorHex("#FFD700")
                    .build();
            ReflectionTestUtils.setField(saved, "id", 1L);
            given(invitationBackgroundRepository.save(any(InvitationBackground.class))).willReturn(saved);

            InvitationBackgroundCreateResponse result = invitationBackgroundService.create(request);

            assertThat(result.id()).isEqualTo(1L);
            verify(invitationBackgroundRepository).save(any(InvitationBackground.class));
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("solidColorHex를 포함해 배경을 수정한다")
        void update_success() {
            Long id = 1L;
            InvitationBackground existing = InvitationBackground.builder()
                    .name("파스텔 옐로우")
                    .hexCode("#FFFFE0")
                    .solidColorHex("#FFD700")
                    .build();
            ReflectionTestUtils.setField(existing, "id", id);
            given(invitationBackgroundRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(existing));

            InvitationBackgroundRequest request = new InvitationBackgroundRequest("스카이 블루", "#E0F7FA", "#00ACC1");

            InvitationBackgroundResponse result = invitationBackgroundService.update(id, request);

            assertThat(result.name()).isEqualTo("스카이 블루");
            assertThat(result.hexCode()).isEqualTo("#E0F7FA");
            assertThat(result.solidColorHex()).isEqualTo("#00ACC1");
        }
    }
}
