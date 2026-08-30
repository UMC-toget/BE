package com.example.toget.domain.bank.entity;

import com.example.toget.global.entity.BaseEntity;
import com.example.toget.global.enums.BankName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 마스터 데이터 엔티티 — banks 테이블 매핑.
 *
 * <p>[왜 enum이 있는데 테이블도 두는가]
 * {@link BankName} enum은 "어떤 은행 코드가 유효한가"(입력 검증)를 담당하고,
 * 이 테이블은 "그 은행을 화면에 어떻게 보여줄 것인가"(아이콘·표시명·노출순서·활성여부)를 담당한다.
 * 아이콘 교체나 표시명 수정이 코드 배포 없이 DB만으로 끝나도록 분리한 것이다.
 *
 * <p>[행의 생성 주체는 BankSeeder 하나뿐]
 * banks 행은 {@code BankSeeder}가 {@code BankName.values()}를 순회해서만 만든다.
 * 덕분에 "GET /api/v1/banks가 주는 목록 = 계좌 등록이 받아주는 값"이 항상 일치한다.
 * enum에 없는 은행을 수동 INSERT하면 목록에는 보이지만 계좌 등록이 400으로 거부되므로 금지.
 *
 * <p>캐릭터·초대장 배경과 달리 soft delete(deleted_at)를 쓰지 않고 {@code isActive}로 노출을 제어한다.
 * 은행은 서비스에서 사라지는 개념이 아니라 "신규 선택만 막고 기존 계좌는 유지"가 필요하기 때문이다.
 */
@Entity
@Table(name = "banks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자, 외부 사용은 차단
public class Bank extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_id")
    private Long id;

    /**
     * 은행 코드 — BankName enum과 1:1. user_accounts.bank_name과 같은 문자열이 저장된다.
     * unique 제약이 시더의 멱등성(같은 코드 재INSERT 방지)을 DB 레벨에서 한 번 더 보장한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private BankName code;

    /** 한국어 표시명 — "카카오뱅크". enum의 displayName으로 시드되지만 이후 관리자가 수정 가능 */
    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    /**
     * 은행 아이콘 S3 URL.
     * nullable인 이유: 아이콘을 아직 확보하지 못한 은행이 있어도 서비스가 동작해야 한다.
     * null이면 프론트가 fallback 아이콘을 표시한다.
     */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /** 은행 선택 바텀시트 노출 순서 (오름차순) */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * 노출 여부 — false면 목록 API에서 제외되어 신규 계좌 등록 시 선택할 수 없다.
     * 이미 이 은행으로 등록된 계좌는 그대로 유지된다(합병·영업중단 은행 대응).
     *
     * <p>필드명을 isActive가 아니라 active로 둔 이유: boolean 필드를 isXxx로 지으면
     * JavaBeans 규약상 프로퍼티명이 active로 해석되어, Spring Data 쿼리 메서드가
     * isActive와 active 중 무엇을 찾을지 애매해진다. 필드는 active로 두고 컬럼만 is_active로 매핑한다.
     * (Lombok이 만드는 게터는 어느 쪽이든 isActive()로 동일하다)
     */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder
    private Bank(BankName code, String displayName, String iconUrl, int sortOrder, boolean active) {
        this.code = code;
        this.displayName = displayName;
        this.iconUrl = iconUrl;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    /**
     * 아이콘 URL만 채운다 — 시더의 초기 백필 전용.
     * 이미 값이 있는 행에는 호출하지 않는다(관리자가 손으로 바꾼 값을 시더가 덮어쓰지 않도록).
     */
    public void fillIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    /** null인 필드는 건드리지 않는 부분 수정(PATCH) — 트랜잭션 커밋 시 dirty checking으로 UPDATE된다 */
    public void update(String displayName, String iconUrl, Integer sortOrder, Boolean isActive) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (iconUrl != null) {
            this.iconUrl = iconUrl;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (isActive != null) {
            this.active = isActive;
        }
    }
}
