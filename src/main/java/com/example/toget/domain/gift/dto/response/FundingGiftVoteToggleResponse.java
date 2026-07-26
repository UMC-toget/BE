package com.example.toget.domain.gift.dto.response;


public record FundingGiftVoteToggleResponse(
        Long fundingGiftId,
        boolean voted
) {}