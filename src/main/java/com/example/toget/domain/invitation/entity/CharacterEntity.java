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
 * 초대장 카드에 표시되는 캐릭터 엔티티
 *
 *  클래스명(CharacterEntity) 예외:
 *  java.lang.Character(래퍼 클래스)와 이름이 겹쳐 같은 파일에서 혼동을 유발할 수 있어 예외적으로 Entity 접미사를 붙임.
 */
@Entity
@Table(name = "characters")
@Getter
// JPA는 엔티티 생성 시 기본 생성자가 필요.
// 외부에서 무분별하게 객체를 생성하지 못하도록 protected로 제한한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CharacterEntity extends BaseEntity {

    @Id
    // PK 생성 -> MySQL AUTO_INCREMENT에 ID 생성을 위임
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 캐릭터 이름
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // 캐릭터 이미지 URL
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Builder
    private CharacterEntity(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    // 캐릭터 정보 전체 수정
    public void update(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    /**
     * soft delete — 레코드를 지우지 않고 deleted_at에 삭제 시각만 기록.
     * hard delete를 쓰지 않는 이유: 이미 발행된 초대장(invitation_cards)이 이 캐릭터를 FK로
     * 참조하고 있어, 실제 삭제 시 FK 제약 위반이나 기존 초대장 깨짐이 발생하기 때문.
     * 조회 API에서는 deleted_at IS NULL 조건으로 걸러서 신규 선택지에서만 제외된다.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
