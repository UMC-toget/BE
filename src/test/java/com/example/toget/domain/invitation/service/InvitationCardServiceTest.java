package com.example.toget.domain.invitation.service;

import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.invitation.dto.InvitationCardUpdateRequest;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationCard;
import com.example.toget.domain.invitation.exception.InvitationException;
import com.example.toget.domain.invitation.exception.code.InvitationErrorCode;
import com.example.toget.domain.invitation.repository.CharacterRepository;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import com.example.toget.domain.invitation.repository.InvitationCardRepository;
import com.example.toget.domain.user.service.ActiveUserReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * InvitationCardService 단위 테스트.
 * 리포지토리는 mock으로 대체하고 권한 검증(403)과 soft delete 검증(404) 핵심 로직만 검증한다.
 * 회귀 방지 목적 — 검증 순서나 조회 메서드(findByIdAndDeletedAtIsNull)가 바뀌면 실패해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class InvitationCardServiceTest {

    @Mock
    private InvitationCardRepository invitationCardRepository;
    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private CharacterRepository characterRepository;
    @Mock
    private InvitationBackgroundRepository invitationBackgroundRepository;
    @Mock
    private ActiveUserReader activeUserReader;

    @InjectMocks
    private InvitationCardService invitationCardService;

    private static final Long CALLER_ID = 1L;
    private static final Long FUNDING_ID = 10L;

    private final InvitationCardUpdateRequest request =
            new InvitationCardUpdateRequest(2L, 3L, "수정된 제목", "수정된 본문");

    /** 지정한 소유자(userId)의 펀딩 생성 — MY_GIFT는 계좌 필수라 임의 값 주입 */
    private Funding fundingOwnedBy(Long ownerId) {
        return Funding.createMyGift(ownerId, 100L, "생일 펀딩", "홍길동",
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 14), "소개글", "https://image.com/thumb.png", 1_000_000L);
    }

    @Test
    @DisplayName("펀딩 개최자가 아니면 INVITATION_FORBIDDEN(403)을 던지고 초대장 조회까지 가지 않는다")
    void notHost_throwsForbidden() {
        // given: 펀딩은 존재하지만 소유자가 호출자(1L)와 다른 사용자(999L)
        given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(fundingOwnedBy(999L)));

        // when & then
        assertThatThrownBy(() -> invitationCardService.update(CALLER_ID, FUNDING_ID, request))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getCode())
                .isEqualTo(InvitationErrorCode.INVITATION_FORBIDDEN);

        // 권한 검증이 초대장 조회보다 먼저 이뤄지므로 카드 조회는 호출되지 않아야 한다
        verify(invitationCardRepository, never()).findByFundingId(any());
    }

    @Test
    @DisplayName("대상 펀딩이 없거나 soft delete되었으면 INVITATION_NOT_FOUND(404)를 던진다")
    void fundingNotFoundOrDeleted_throwsNotFound() {
        // findByIdAndDeletedAtIsNull은 미존재/삭제된 펀딩을 모두 빈 결과로 반환하므로 한 케이스로 검증한다
        given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> invitationCardService.update(CALLER_ID, FUNDING_ID, request))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getCode())
                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND);
    }

    @Test
    @DisplayName("soft delete된 캐릭터를 선택하면 CHARACTER_NOT_FOUND(404)를 던진다")
    void softDeletedCharacter_throwsNotFound() {
        // given: 개최자 본인 + 초대장 존재, 그러나 캐릭터는 soft delete되어 조회되지 않음
        given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(fundingOwnedBy(CALLER_ID)));
        given(invitationCardRepository.findByFundingId(FUNDING_ID)).willReturn(Optional.of(cardStub()));
        given(characterRepository.findByIdAndDeletedAtIsNull(request.characterId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> invitationCardService.update(CALLER_ID, FUNDING_ID, request))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getCode())
                .isEqualTo(InvitationErrorCode.CHARACTER_NOT_FOUND);
    }

    @Test
    @DisplayName("soft delete된 배경을 선택하면 BACKGROUND_NOT_FOUND(404)를 던진다")
    void softDeletedBackground_throwsNotFound() {
        // given: 캐릭터는 정상 조회되지만 배경은 soft delete되어 조회되지 않음
        given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(fundingOwnedBy(CALLER_ID)));
        given(invitationCardRepository.findByFundingId(FUNDING_ID)).willReturn(Optional.of(cardStub()));
        given(characterRepository.findByIdAndDeletedAtIsNull(request.characterId()))
                .willReturn(Optional.of(CharacterEntity.builder().name("캐릭터").imageUrl("https://img/c.png").build()));
        given(invitationBackgroundRepository.findByIdAndDeletedAtIsNull(request.backgroundId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> invitationCardService.update(CALLER_ID, FUNDING_ID, request))
                .isInstanceOf(InvitationException.class)
                .extracting(e -> ((InvitationException) e).getCode())
                .isEqualTo(InvitationErrorCode.BACKGROUND_NOT_FOUND);
    }

    /** 조회 대상으로만 쓰이는 초대장 스텁 — 필드 값 자체는 검증에 관여하지 않는다 */
    private InvitationCard cardStub() {
        return InvitationCard.builder()
                .fundingId(FUNDING_ID)
                .title("기존 제목")
                .content("기존 본문")
                .url("https://toget.app/i/abc")
                .build();
    }
}
