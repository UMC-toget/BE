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
}
