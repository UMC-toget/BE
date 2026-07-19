package com.example.toget.domain.funding.entity;

import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축하 메시지 카드용 배경 색상 — contribution_backgrounds 테이블 매핑.
 * 관리자가 관리하는 마스터 데이터로, funding_contributions.background_id가 참조한다.
 */
@Entity
@Table(name = "contribution_backgrounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionBackground extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "hex_code", nullable = false, length = 10)
    private String hexCode;

    @Builder
    private ContributionBackground(String name, String hexCode) {
        validate(name, hexCode);
        this.name = name;
        this.hexCode = hexCode;
    }

    public static ContributionBackground create(String name, String hexCode) {
        return ContributionBackground.builder()
                .name(name)
                .hexCode(hexCode)
                .build();
    }

    public void update(String name, String hexCode) {
        validate(name, hexCode);
        this.name = name;
        this.hexCode = hexCode;
    }

    private static void validate(String name, String hexCode) {
        if (name == null || name.isBlank()) {
            throw new ContributionException(ContributionErrorCode.INVALID_BACKGROUND_NAME);
        }
        if (hexCode == null || !hexCode.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new ContributionException(ContributionErrorCode.INVALID_HEX_CODE);
        }
    }
}