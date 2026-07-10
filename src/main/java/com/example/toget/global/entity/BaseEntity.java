package com.example.toget.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @CreatedDate // 저장 이벤트가 발생했을때 해당 칼럼값을 현재 시각으로 저장
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 수정 이벤트가 발생했을때 해당 칼럼값을 현재 시각으로 저장
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // private → protected: soft delete가 필요한 엔티티(캐릭터/초대장 배경 등)가
    // 자신의 delete() 메서드에서 이 필드를 직접 기록할 수 있도록 접근만 열어둔다.
    // BaseEntity에 공통 삭제 메서드를 두지 않는 이유: 모든 엔티티가 soft delete 대상인 것처럼 보이는 것을 방지.
    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;
}
