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
import com.example.toget.domain.invitation.dto.InvitationCardResponse;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.repository.UserRepository;
import com.example.toget.domain.user.service.ActiveUserReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    @Mock
    private UserRepository userRepository;

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

    /**
     * 초대장 카드 조회(GET) — 비회원이 초대장 링크로 들어왔을 때 쓰는 공개 API.
     *
     * [설계 포인트]
     *  - 로그인 정보를 받지 않으므로 권한 검증이 없다. 대신 개설자 이름 조회가 추가된다.
     *  - 개설자가 탈퇴/삭제되어도 초대장 자체는 보여야 한다. 이름만 null로 내린다.
     */
    @Nested
    @DisplayName("초대장 카드 조회")
    class GetInvitationCard {

        private static final Long OWNER_ID = 7L;

        /** 캐릭터·배경이 채워진 카드 — 컨버터가 두 연관 엔티티의 id를 읽으므로 필수 */
        private InvitationCard cardWithRelations() {
            return InvitationCard.builder()
                    .fundingId(FUNDING_ID)
                    .character(CharacterEntity.builder().name("고양이").imageUrl("https://img/cat.png").build())
                    .background(InvitationBackground.builder().name("핑크").hexCode("#FFB6C1").build())
                    .title("생일 초대장이 도착했어요")
                    .content("안뇽!! 내가 이번 생일에 진짜 필요한 선물을 사고 싶은데...")
                    .url("https://toget.app/i/abc")
                    .build();
        }

        @Test
        @DisplayName("정상 조회 시 개설자 이름이 함께 내려간다")
        void success_withCreatorName() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(fundingOwnedBy(OWNER_ID)));
            given(invitationCardRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(cardWithRelations()));
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(
                    User.builder().oAuthProvider(OAuthProvider.GOOGLE).oAuthId("oauth-1").name("희주").build()
            ));

            InvitationCardResponse result = invitationCardService.getInvitationCard(FUNDING_ID);

            assertThat(result.creatorName()).isEqualTo("희주");
            assertThat(result.title()).isEqualTo("생일 초대장이 도착했어요");
            assertThat(result.content()).startsWith("안뇽!!");
        }

        /**
         * 개설자 조회에 ActiveUserReader를 쓰면 탈퇴 시 401이 던져진다.
         * 로그인도 하지 않은 방문자에게 "인증 정보가 올바르지 않습니다"가 나가면 안 되므로,
         * UserRepository로 직접 조회해 이름만 null로 내린다.
         */
        @Test
        @DisplayName("개설자를 찾을 수 없어도 초대장은 조회되고 이름만 null이 된다")
        void success_creatorNameNull_whenUserMissing() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(fundingOwnedBy(OWNER_ID)));
            given(invitationCardRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(cardWithRelations()));
            given(userRepository.findById(OWNER_ID)).willReturn(Optional.empty());

            InvitationCardResponse result = invitationCardService.getInvitationCard(FUNDING_ID);

            assertThat(result.creatorName()).isNull();
            assertThat(result.title()).isEqualTo("생일 초대장이 도착했어요");
        }

        @Test
        @DisplayName("펀딩이 없거나 soft delete되었으면 INVITATION_NOT_FOUND(404)를 던진다")
        void fundingNotFoundOrDeleted() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> invitationCardService.getInvitationCard(FUNDING_ID))
                    .isInstanceOf(InvitationException.class)
                    .extracting(e -> ((InvitationException) e).getCode())
                    .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND);

            verify(invitationCardRepository, never()).findByFundingId(any());
        }

        /**
         * 펀딩 생성 시 초대장도 함께 만들어지고 funding_id에 UNIQUE 제약이 있어
         * 정상 데이터에선 도달할 수 없는 분기다. 500(NPE) 대신 404로 드러내기 위한 방어 코드이므로,
         * 누군가 "필요 없는 분기"로 판단해 제거하면 이 테스트가 실패한다.
         */
        @Test
        @DisplayName("초대장 행이 없으면 INVITATION_NOT_FOUND(404)를 던진다 (데이터 이상 방어)")
        void invitationCardMissing() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(fundingOwnedBy(OWNER_ID)));
            given(invitationCardRepository.findByFundingId(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> invitationCardService.getInvitationCard(FUNDING_ID))
                    .isInstanceOf(InvitationException.class)
                    .extracting(e -> ((InvitationException) e).getCode())
                    .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND);

            verify(userRepository, never()).findById(any());
        }
    }
}
