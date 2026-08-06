package com.example.toget.domain.bank.service;

import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.domain.bank.repository.BankRepository;
import com.example.toget.global.config.BankIconProperties;
import com.example.toget.global.enums.BankName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * banks 테이블 시더 — 애플리케이션 기동 시 {@link BankName} enum을 banks 행으로 동기화한다.
 *
 * <p>[왜 data.sql이 아니라 시더인가]
 * 은행 목록의 원본은 BankName enum이다. data.sql로 관리하면 enum에 은행을 추가할 때
 * SQL 파일도 같이 고쳐야 해서 두 곳이 어긋날 수 있다. enum을 순회해 채우면 이중 관리가 사라진다.
 *
 * <p>[멱등성 — 여러 번 실행돼도 안전]
 * 서버는 배포·재기동으로 몇 번이든 다시 뜬다. 그래서 이 시더는 세 가지 규칙만 지킨다.
 * <ol>
 *   <li>코드가 없으면 → INSERT</li>
 *   <li>코드가 있고 icon_url이 비어 있으면 → 아이콘 URL만 채움(초기 백필)</li>
 *   <li>코드가 있고 icon_url이 이미 있으면 → 손대지 않음</li>
 * </ol>
 * 3번이 중요하다. 관리자가 API로 바꾼 아이콘·표시명·노출순서를 서버가 재기동될 때마다
 * 시드 기본값으로 되돌려 버리면 "무배포로 아이콘 교체"라는 이 기능의 목적 자체가 깨진다.
 * 같은 이유로 displayName·sortOrder·isActive도 INSERT 시점에만 기록한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankSeeder implements ApplicationRunner {

    private final BankRepository bankRepository;
    private final BankIconProperties bankIconProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 22행뿐이라 전건을 한 번에 읽어 Map으로 만든다 — enum마다 findByCode를 부르면 쿼리가 22번 나간다
        Map<BankName, Bank> existing = bankRepository.findAll().stream()
                .collect(Collectors.toMap(Bank::getCode, Function.identity()));

        List<Bank> toInsert = new ArrayList<>();
        int backfilled = 0;

        for (BankName bankName : BankName.values()) {
            String iconUrl = bankIconProperties.resolveUrl(bankName.name());
            Bank bank = existing.get(bankName);

            if (bank == null) {
                toInsert.add(Bank.builder()
                        .code(bankName)
                        .displayName(bankName.getDisplayName())
                        // 선언 순서를 그대로 노출 순서로 쓴다(시중은행 → 인터넷은행 → 지방은행).
                        // 순서를 바꾸고 싶으면 enum을 건드리지 말고 관리자 PATCH API로 sortOrder를 수정한다.
                        .sortOrder(bankName.ordinal())
                        .iconUrl(iconUrl)
                        .active(true)
                        .build());
                continue;
            }

            // 기존 행의 아이콘이 비어 있을 때만 채운다 — 아이콘 업로드가 스키마 배포보다 늦어지는 경우를 위한 백필
            if (bank.getIconUrl() == null && iconUrl != null) {
                bank.fillIconUrl(iconUrl);
                backfilled++;
            }
        }

        if (!toInsert.isEmpty()) {
            bankRepository.saveAll(toInsert);
        }

        if (!bankIconProperties.isConfigured()) {
            log.warn("bank-icon.base-url이 설정되지 않아 은행 아이콘 URL을 비워 둡니다. "
                    + "아이콘 업로드 후 환경변수 BANK_ICON_BASE_URL을 설정하고 재기동하면 자동으로 채워집니다.");
        }
        log.info("은행 시드 완료 — 신규 {}건, 아이콘 백필 {}건, 전체 {}건",
                toInsert.size(), backfilled, BankName.values().length);
    }
}
