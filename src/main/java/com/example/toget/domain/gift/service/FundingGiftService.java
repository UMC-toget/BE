package com.example.toget.domain.gift.service;

import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.enums.FundingRole;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.repository.FundingGiftCommentRepository;
import com.example.toget.domain.funding.repository.FundingGiftVoteRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.gift.dto.request.*;
import com.example.toget.domain.gift.dto.response.*;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.gift.entity.FundingGiftComment;
import com.example.toget.domain.gift.entity.FundingGiftVote;
import com.example.toget.domain.gift.enums.FundingGiftStatus;
import com.example.toget.domain.gift.exception.FundingGiftException;
import com.example.toget.domain.gift.exception.code.FundingGiftErrorCode;
import com.example.toget.domain.gift.repository.*;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundingGiftService {

    private final FundingRepository fundingRepository;
    private final FundingMemberRepository fundingMemberRepository;
    private final FundingGiftRepository fundingGiftRepository;
    private final FundingGiftVoteRepository fundingGiftVoteRepository;
    private final FundingGiftCommentRepository fundingGiftCommentRepository;
    private final UserRepository userRepository;

    private static final int MAX_VOTE_COUNT = 3;

    @Transactional(readOnly = true)
    public FundingGiftCandidateListResponse getCandidates(Long fundingId, Long viewerId) {
        List<FundingGift> candidates = fundingGiftRepository
                .findAllByFundingIdAndStatus(fundingId, FundingGiftStatus.CANDIDATE);
        List<Long> giftIds = candidates.stream().map(FundingGift::getId).toList();

        Map<Long, Long> voteCountMap = giftIds.isEmpty()
                ? Map.of()
                : fundingGiftVoteRepository.countVotesByGiftIds(giftIds).stream()
                .collect(Collectors.toMap(
                        FundingGiftVoteRepository.GiftVoteCountProjection::getGiftId,
                        FundingGiftVoteRepository.GiftVoteCountProjection::getVoteCount
                ));

        List<Long> votedGiftIds = List.of();
        if (viewerId != null) {
            FundingMember viewerMember = fundingMemberRepository
                    .findByFundingIdAndUserId(fundingId, viewerId).orElse(null);
            if (viewerMember != null) {
                votedGiftIds = fundingGiftVoteRepository.findAllByFundingMemberId(viewerMember.getId())
                        .stream().map(FundingGiftVote::getFundingGiftId).toList();
            }
        }

        List<FundingGiftCandidateListResponse.CandidateItem> items = candidates.stream()
                .map(g -> new FundingGiftCandidateListResponse.CandidateItem(
                        g.getId(), g.getImageUrl(), g.getName(), g.getPrice(),
                        voteCountMap.getOrDefault(g.getId(), 0L)
                ))
                .toList();

        return new FundingGiftCandidateListResponse(votedGiftIds, items);
    }

    @Transactional
    public FundingGiftCandidateCreateResponse createCandidate(
            Long userId, Long fundingId, FundingGiftCandidateCreateRequest request
    ) {
        FundingMember member = getCreatorOrAdminMember(fundingId, userId);

        FundingGift gift = FundingGift.createCandidate(
                fundingId, member.getId(), request.giftName(), request.giftPrice(),
                request.giftPurchaseUrl(), request.giftImageUrl(), request.note()
        );
        FundingGift saved = fundingGiftRepository.save(gift);

        return new FundingGiftCandidateCreateResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public FundingGiftCandidateDetailResponse getCandidateDetail(Long fundingId, Long fundingGiftId, Long viewerId) {
        FundingGift gift = getGiftOrThrow(fundingId, fundingGiftId);
        long voteCount = fundingGiftVoteRepository.countByFundingGiftId(fundingGiftId);
        String registrantName = resolveRegistrantName(gift.getFundingMemberId());

        boolean isVotedByViewer = false;
        if (viewerId != null) {
            FundingMember viewerMember = fundingMemberRepository
                    .findByFundingIdAndUserId(fundingId, viewerId).orElse(null);
            if (viewerMember != null) {
                isVotedByViewer = fundingGiftVoteRepository
                        .findByFundingMemberIdAndFundingGiftId(viewerMember.getId(), fundingGiftId).isPresent();
            }
        }

        List<FundingGiftComment> comments = fundingGiftCommentRepository
                .findAllByFundingGiftIdOrderByCreatedAtAsc(fundingGiftId);
        List<FundingGiftCandidateDetailResponse.CommentItem> commentItems = toCommentItems(comments);

        return new FundingGiftCandidateDetailResponse(
                gift.getId(), gift.getImageUrl(), gift.getName(), gift.getPrice(), voteCount,
                gift.getPurchaseUrl(), registrantName, gift.getNote(), isVotedByViewer, commentItems
        );
    }

    @Transactional
    public FundingGiftVoteToggleResponse toggleVote(Long userId, Long fundingId, Long fundingGiftId) {
        FundingGift gift = getGiftOrThrow(fundingId, fundingGiftId);
        if (gift.getStatus() == FundingGiftStatus.SELECTED) {
            throw new FundingGiftException(FundingGiftErrorCode.GIFT_ALREADY_SELECTED);
        }

        FundingMember member = getMemberOrThrow(fundingId, userId);

        var existing = fundingGiftVoteRepository
                .findByFundingMemberIdAndFundingGiftId(member.getId(), fundingGiftId);

        if (existing.isPresent()) {
            fundingGiftVoteRepository.delete(existing.get());
            return new FundingGiftVoteToggleResponse(fundingGiftId, false);
        }

        long currentVoteCount = fundingGiftVoteRepository.findAllByFundingMemberId(member.getId()).size();
        if (currentVoteCount >= MAX_VOTE_COUNT) {
            throw new FundingGiftException(FundingGiftErrorCode.VOTE_LIMIT_EXCEEDED);
        }

        fundingGiftVoteRepository.save(FundingGiftVote.create(fundingGiftId, member.getId()));
        return new FundingGiftVoteToggleResponse(fundingGiftId, true);
    }

    @Transactional
    public FundingGiftCommentCreateResponse createComment(
            Long userId, Long fundingId, Long fundingGiftId, FundingGiftCommentCreateRequest request
    ) {
        getGiftOrThrow(fundingId, fundingGiftId);
        FundingMember member = getMemberOrThrow(fundingId, userId);

        FundingGiftComment comment = FundingGiftComment.create(fundingGiftId, member.getId(), request.content());
        FundingGiftComment saved = fundingGiftCommentRepository.save(comment);

        return new FundingGiftCommentCreateResponse(saved.getId());
    }

    // --- private helpers ---

    private FundingGift getGiftOrThrow(Long fundingId, Long fundingGiftId) {
        FundingGift gift = fundingGiftRepository.findById(fundingGiftId)
                .orElseThrow(() -> new FundingGiftException(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND));
        if (!gift.getFundingId().equals(fundingId)) {
            throw new FundingGiftException(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND);
        }
        return gift;
    }

    private FundingMember getMemberOrThrow(Long fundingId, Long userId) {
        return fundingMemberRepository.findByFundingIdAndUserId(fundingId, userId)
                .orElseThrow(() -> new FundingGiftException(FundingGiftErrorCode.NOT_FUNDING_MEMBER));
    }

    private FundingMember getCreatorOrAdminMember(Long fundingId, Long userId) {
        FundingMember member = getMemberOrThrow(fundingId, userId);
        if (member.getRole() == FundingRole.PARTICIPANT) {
            throw new FundingGiftException(FundingGiftErrorCode.NOT_CREATOR_OR_ADMIN);
        }
        return member;
    }

    private String resolveRegistrantName(Long fundingMemberId) {
        if (fundingMemberId == null) {
            return null;
        }
        FundingMember member = fundingMemberRepository.findById(fundingMemberId).orElse(null);
        if (member == null) {
            return null;
        }
        User user = userRepository.findById(member.getUserId()).orElse(null);
        return user == null ? null : user.getName();
    }

    private List<FundingGiftCandidateDetailResponse.CommentItem> toCommentItems(List<FundingGiftComment> comments) {
        List<Long> memberIds = comments.stream().map(FundingGiftComment::getFundingMemberId).distinct().toList();
        Map<Long, FundingMember> memberMap = fundingMemberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(FundingMember::getId, Function.identity()));

        List<Long> userIds = memberMap.values().stream().map(FundingMember::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return comments.stream()
                .map(c -> {
                    FundingMember member = memberMap.get(c.getFundingMemberId());
                    User user = member == null ? null : userMap.get(member.getUserId());
                    return new FundingGiftCandidateDetailResponse.CommentItem(
                            c.getId(),
                            user == null ? null : user.getName(),
                            user == null ? null : user.getProfileImageUrl(),
                            c.getContent()
                    );
                })
                .toList();
    }
}