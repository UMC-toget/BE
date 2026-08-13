package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.request.ContributionBackgroundRequest;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundCreateResponse;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundResponse;
import com.example.toget.domain.funding.entity.ContributionBackground;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContributionBackgroundService")
class ContributionBackgroundServiceTest {

    @Mock
    private ContributionBackgroundRepository contributionBackgroundRepository;

    @Mock
    private FundingContributionRepository fundingContributionRepository;

    @InjectMocks
    private ContributionBackgroundService contributionBackgroundService;

    @Nested
    @DisplayName("전체 조회")
    class GetAll {

        @Test
        @DisplayName("데이터가 있으면 응답 목록으로 변환해 반환한다")
        void getAll_success() {
            // given
            ContributionBackground background = ContributionBackground.create("파스텔 핑크", "#FFB6C1");
            ReflectionTestUtils.setField(background, "id", 1L);
            given(contributionBackgroundRepository.findAll()).willReturn(List.of(background));

            // when
            List<ContributionBackgroundResponse> result = contributionBackgroundService.getAll();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("파스텔 핑크");
            assertThat(result.get(0).hexCode()).isEqualTo("#FFB6C1");
        }

        @Test
        @DisplayName("데이터가 없으면 빈 리스트를 반환한다")
        void getAll_empty() {
            // given
            given(contributionBackgroundRepository.findAll()).willReturn(List.of());

            // when
            List<ContributionBackgroundResponse> result = contributionBackgroundService.getAll();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 요청이면 생성된 ID를 반환한다")
        void create_success() {
            // given
            ContributionBackgroundRequest request = new ContributionBackgroundRequest("파스텔 핑크", "#FFB6C1");
            ContributionBackground saved = ContributionBackground.create("파스텔 핑크", "#FFB6C1");
            ReflectionTestUtils.setField(saved, "id", 1L);
            given(contributionBackgroundRepository.save(any(ContributionBackground.class))).willReturn(saved);

            // when
            ContributionBackgroundCreateResponse result = contributionBackgroundService.create(request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            verify(contributionBackgroundRepository).save(any(ContributionBackground.class));
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("존재하는 ID면 정상 수정된다")
        void update_success() {
            // given
            Long id = 1L;
            ContributionBackground existing = ContributionBackground.create("파스텔 핑크", "#FFB6C1");
            ReflectionTestUtils.setField(existing, "id", id);
            given(contributionBackgroundRepository.findById(id)).willReturn(Optional.of(existing));

            ContributionBackgroundRequest request = new ContributionBackgroundRequest("핫 핑크", "#FF69B4");

            // when
            ContributionBackgroundResponse result = contributionBackgroundService.update(id, request);

            // then
            assertThat(result.name()).isEqualTo("핫 핑크");
            assertThat(result.hexCode()).isEqualTo("#FF69B4");
        }

        @Test
        @DisplayName("존재하지 않는 ID면 BACKGROUND_NOT_FOUND 예외가 발생한다")
        void update_fail_notFound() {
            // given
            Long id = 999L;
            given(contributionBackgroundRepository.findById(id)).willReturn(Optional.empty());

            ContributionBackgroundRequest request = new ContributionBackgroundRequest("핫 핑크", "#FF69B4");

            // when & then
            assertThatThrownBy(() -> contributionBackgroundService.update(id, request))
                    .isInstanceOf(ContributionException.class)
                    .extracting(e -> ((ContributionException) e).getCode())
                    .isEqualTo(ContributionErrorCode.BACKGROUND_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("존재하는 ID면 정상 삭제된다")
        void delete_success() {
            // given
            Long id = 1L;
            ContributionBackground existing = ContributionBackground.create("파스텔 핑크", "#FFB6C1");
            ReflectionTestUtils.setField(existing, "id", id);
            given(contributionBackgroundRepository.findById(id)).willReturn(Optional.of(existing));
            given(fundingContributionRepository.existsByBackgroundId(id)).willReturn(false);

            // when
            contributionBackgroundService.delete(id);

            // then
            verify(contributionBackgroundRepository).delete(existing);
        }


        @Test
        @DisplayName("이미 사용 중인 배경이면 BACKGROUND_IN_USE 예외가 발생하고 delete는 호출되지 않는다")
        void delete_fail_inUse() {
            // given
            Long id = 1L;
            ContributionBackground existing = ContributionBackground.create("파스텔 핑크", "#FFB6C1");
            ReflectionTestUtils.setField(existing, "id", id);
            given(contributionBackgroundRepository.findById(id)).willReturn(Optional.of(existing));
            given(fundingContributionRepository.existsByBackgroundId(id)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> contributionBackgroundService.delete(id))
                    .isInstanceOf(ContributionException.class)
                    .extracting(e -> ((ContributionException) e).getCode())
                    .isEqualTo(ContributionErrorCode.BACKGROUND_IN_USE);

            verify(contributionBackgroundRepository, never()).delete(any());
        }
    }
}