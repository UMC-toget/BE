package com.example.toget.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 관리자 계정 식별 설정 — application.yaml의 admin.accounts 목록을 바인딩한다.
 *
 * <p>[설계 배경] users 테이블에 role 컬럼을 추가하지 않고, 고정 운영되는 관리자 계정들의
 * 식별자만 환경변수로 관리한다. 스키마 변경 없이 즉시 적용할 수 있고,
 * 운영/개발 환경별로 다른 계정을 지정할 수 있다. (issue #154 — 관리자 다중 지원)
 *
 * <p>[왜 userId가 아니라 email인가] userId는 DB auto-increment라서 로컬/개발/운영마다
 * 값이 달라지고 DB를 초기화하면 바뀐다. email은 환경과 무관하게 동일하다.
 *
 * <p>[왜 provider까지 보는가] 이메일만 비교하면, 관리자 이메일 주소를 카카오 계정에
 * 등록한 뒤 카카오로 로그인해 관리자 권한을 얻는 우회가 가능하다.
 * 공급자까지 일치해야 관리자로 인정하여 이 경로를 막는다.
 *
 * @param accounts 관리자 계정 목록. 비어 있으면 관리자 API를 전면 차단한다(fail-closed).
 */
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(List<AdminIdentity> accounts) {

    public AdminProperties {
        // 설정 누락 시 조용히 null이 되지 않도록 기본값을 여기서 확정한다
        if (accounts == null) {
            accounts = List.of();
        }
        // 환경변수 미설정으로 email이 빈 항목이 섞여 들어와도 관리자로 오인하지 않도록 걸러낸다
        accounts = accounts.stream().filter(AdminIdentity::isValid).toList();
    }

    /**
     * 관리자 식별값이 실제로 하나 이상 설정되어 있는지 여부.
     * false면 AdminOnlyInterceptor가 모든 요청을 차단한다 —
     * 설정 누락이 "전면 개방"이 아니라 "전면 차단"으로 이어지게 하기 위함(fail-closed).
     */
    public boolean isConfigured() {
        return !accounts.isEmpty();
    }
}
