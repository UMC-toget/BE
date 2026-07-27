package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FundingMember 목록으로부터 User 정보를 배치 조회하는 공용 헬퍼.
 * FundingService(참여자 관리), FundingQueryService(대시보드) 등 여러 서비스에서
 * 동일한 조회 로직이 필요해 별도 컴포넌트로 분리했다.
 */
@Component
@RequiredArgsConstructor
public class FundingMemberUserResolver {

    private final UserRepository userRepository;

    public Map<Long, User> resolve(List<FundingMember> members) {
        List<Long> userIds = members.stream().map(FundingMember::getUserId).toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}