package com.example.toget.domain.invitation.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 초대장 카드 배경(색상) 엔티티
 */
@Entity
@Table(name = "invitation_backgrounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationBackground extends BaseEntity {

    @Id
    // PK 생성 -> MySQL AUTO_INCREMENT에 ID 생성을 위임
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 배경 이름
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // 배경 색상 hex 코드 (ex. "#FFFFFF"). '#' 포함 여유를 두어 최대 10자로 설정.
    @Column(name = "hex_code", nullable = false, length = 10)
    private String hexCode;

    // 원색 hex 코드 (ex. "#FF007F"). 50% 불투명도/파스텔톤 미적용 진한 원색 Hex 코드
    @Column(name = "solid_color_hex", nullable = false, length = 10)
    private String solidColorHex;

    @Builder
    private InvitationBackground(String name, String hexCode, String solidColorHex) {
        this.name = name;
        this.hexCode = hexCode;
        this.solidColorHex = solidColorHex;
    }

    // 배경 색상 정보 전체 수정
    public void update(String name, String hexCode, String solidColorHex) {
        this.name = name;
        this.hexCode = hexCode;
        this.solidColorHex = solidColorHex;
    }

    /**
     * soft delete — 레코드를 지우지 않고 deleted_at에 삭제 시각만 기록.
     * 이미 발행된 초대장(invitation_cards)이 이 배경을 FK로 참조하므로 hard delete 시
     * FK 제약 위반/초대장 깨짐 방지 목적. 조회 API는 deleted_at IS NULL 조건으로 걸러낸다.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
