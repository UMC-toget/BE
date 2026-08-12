package com.example.toget.domain.workspace.service;

import com.example.toget.domain.gift.repository.IndividualFundingDraftGiftRepository;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.service.ActiveUserReader;
import com.example.toget.domain.workspace.entity.IndividualFundingDraft;
import com.example.toget.domain.workspace.exception.WorkspaceException;
import com.example.toget.domain.workspace.exception.code.WorkspaceErrorCode;
import com.example.toget.domain.workspace.repository.IndividualFundingDraftRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IndividualFundingDraftServiceTest {

    @Mock
    private IndividualFundingDraftRepository individualFundingDraftRepository;

    @Mock
    private IndividualFundingDraftGiftRepository individualFundingDraftGiftRepository;

    @Mock
    private ActiveUserReader activeUserReader;

    @InjectMocks
    private IndividualFundingDraftService individualFundingDraftService;

    @Nested
    @DisplayName("개인 선물 임시 저장 삭제")
    class DeleteDraft {

        @Test
        @DisplayName("본인의 임시 저장 드래프트 및 연동 선물을 성공적으로 삭제한다")
        void deleteSuccess() {
            Long userId = 1L;
            Long draftId = 5L;
            User activeUser = mock(User.class);
            IndividualFundingDraft draft = IndividualFundingDraft.builder()
                    .id(draftId)
                    .userId(userId)
                    .build();

            given(activeUserReader.getActiveUser(userId)).willReturn(activeUser);
            given(individualFundingDraftRepository.findById(draftId)).willReturn(Optional.of(draft));

            individualFundingDraftService.delete(userId, draftId);

            verify(individualFundingDraftGiftRepository).deleteByMyDraftId(draftId);
            verify(individualFundingDraftRepository).delete(draft);
        }

        @Test
        @DisplayName("존재하지 않는 draftId로 삭제 시 DRAFT_NOT_FOUND 예외가 발생한다")
        void deleteNotFound() {
            Long userId = 1L;
            Long draftId = 999L;
            User activeUser = mock(User.class);

            given(activeUserReader.getActiveUser(userId)).willReturn(activeUser);
            given(individualFundingDraftRepository.findById(draftId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> individualFundingDraftService.delete(userId, draftId))
                    .isInstanceOf(WorkspaceException.class)
                    .extracting("code")
                    .isEqualTo(WorkspaceErrorCode.DRAFT_NOT_FOUND);
        }

        @Test
        @DisplayName("타인의 임시 저장 드래프트 삭제 시 DRAFT_FORBIDDEN 예외가 발생한다")
        void deleteForbidden() {
            Long userId = 1L;
            Long otherUserId = 2L;
            Long draftId = 5L;
            User activeUser = mock(User.class);
            IndividualFundingDraft draft = IndividualFundingDraft.builder()
                    .id(draftId)
                    .userId(otherUserId)
                    .build();

            given(activeUserReader.getActiveUser(userId)).willReturn(activeUser);
            given(individualFundingDraftRepository.findById(draftId)).willReturn(Optional.of(draft));

            assertThatThrownBy(() -> individualFundingDraftService.delete(userId, draftId))
                    .isInstanceOf(WorkspaceException.class)
                    .extracting("code")
                    .isEqualTo(WorkspaceErrorCode.DRAFT_FORBIDDEN);
        }
    }
}
