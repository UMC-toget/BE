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

    @Builder
    private InvitationBackground(String name, String hexCode) {
        this.name = name;
        this.hexCode = hexCode;
    }
}
