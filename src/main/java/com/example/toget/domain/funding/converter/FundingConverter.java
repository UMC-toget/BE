package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.dto.MyFundingListResponse.MyFundingSummary;
import com.example.toget.domain.funding.entity.Funding;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

/**
 * Funding 엔티티 → 응답 DTO 변환 전담 클래스.
 * collectedAmount는 엔티티에 없는 계산 값이라 합산 맵을 함께 받아 조립한다.
 * 상태가 없으므로 static 메서드로만 구성 (private 생성자로 인스턴스화 차단) — UserConverter와 동일한 규칙.
 */
public class FundingConverter {

    private FundingConverter() {
    }

    public static MyFundingListResponse toMyFundingListResponse(Slice<Funding> fundings,
                                                                Map<Long, Long> collectedAmounts) {
        List<MyFundingSummary> items = fundings.getContent().stream()
                .map(funding -> toMyFundingSummary(
                        funding,
                        collectedAmounts.getOrDefault(funding.getId(), 0L) // 참여금 없는 펀딩은 0
                ))
                .toList();
        return new MyFundingListResponse(
                items,
                fundings.getNumber(),  // 현재 페이지 번호
                fundings.getSize(),    // 요청한 페이지 크기
                fundings.hasNext()
        );
    }

    private static MyFundingSummary toMyFundingSummary(Funding funding, Long collectedAmount) {
        return new MyFundingSummary(
                funding.getId(),
                funding.getFundingType().name(),
                funding.getTitle(),
                funding.getRecipientName(),
                funding.getTargetAmount(),
                collectedAmount,
                funding.getStatus().name(),
                funding.getEndDate(),
                funding.getThumbnailImageUrl(),
                funding.getCreatedAt()
        );
    }
}
