package com.example.toget.domain.funding.service;


import com.example.toget.domain.funding.converter.ContributionBackgroundConverter;
import com.example.toget.domain.funding.dto.request.ContributionBackgroundRequest;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundCreateResponse;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundResponse;
import com.example.toget.domain.funding.entity.ContributionBackground;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

    @Service
    @RequiredArgsConstructor
    public class ContributionBackgroundService {

        private final ContributionBackgroundRepository contributionBackgroundRepository;

        /** 배경 색상 전체 조회. 데이터가 없으면 빈 리스트 반환 */
        @Transactional(readOnly = true)
        public List<ContributionBackgroundResponse> getAll() {
            return contributionBackgroundRepository.findAll().stream()
                    .map(ContributionBackgroundConverter::toResponse)
                    .toList();
        }

        /** 배경 색상 생성 (관리자용) */
        @Transactional
        public ContributionBackgroundCreateResponse create(ContributionBackgroundRequest request) {
            ContributionBackground background = ContributionBackground.create(request.name(), request.hexCode());
            ContributionBackground saved = contributionBackgroundRepository.save(background);
            return new ContributionBackgroundCreateResponse(saved.getId());
        }

        /** 배경 색상 수정 (관리자용) */
        @Transactional
        public ContributionBackgroundResponse update(Long id, ContributionBackgroundRequest request) {
            ContributionBackground background = getBackgroundOrThrow(id);
            background.update(request.name(), request.hexCode());
            return ContributionBackgroundConverter.toResponse(background);
        }

        /** 배경 색상 삭제 (관리자용) */
        @Transactional
        public void delete(Long id) {
            ContributionBackground background = getBackgroundOrThrow(id);
            contributionBackgroundRepository.delete(background);
        }

        private ContributionBackground getBackgroundOrThrow(Long id) {
            return contributionBackgroundRepository.findById(id)
                    .orElseThrow(() -> new ContributionException(ContributionErrorCode.BACKGROUND_NOT_FOUND));
        }
    }