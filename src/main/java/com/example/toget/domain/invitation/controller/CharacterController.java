package com.example.toget.domain.invitation.controller;

import com.example.toget.domain.invitation.dto.CharacterCreateResponse;
import com.example.toget.domain.invitation.dto.CharacterRequest;
import com.example.toget.domain.invitation.dto.CharacterResponse;
import com.example.toget.domain.invitation.exception.code.InvitationSuccessCode;
import com.example.toget.domain.invitation.service.CharacterService;
import com.example.toget.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 캐릭터(초대장 스킨) CRUD API 컨트롤러.
 * 전체 조회(GET)는 비로그인 허용(SecurityConfig permitAll), 생성/수정/삭제는 로그인 필요.
 * TODO: ADMIN 권한 도입 후 생성/수정/삭제는 관리자 전용으로 제한 (SecurityConfig의 TODO 참고)
 */
@Tag(name = "초대장 API", description = "초대장 및 캐릭터 관련 API")
@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    // 캐릭터 전체 조회 — 초대장 생성 화면의 캐릭터 선택지 목록
    @Operation(summary = "캐릭터 전체 조회", description = "초대장 생성 화면의 캐릭터 선택지 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<CharacterResponse>> getAllCharacters() {
        return ApiResponse.onSuccess(InvitationSuccessCode.CHARACTER_LIST_OK, characterService.getAllCharacters());
    }

    /**
     * 캐릭터 생성.
     *
     * @deprecated 관리자 권한 체계가 아직 없어 로그인 여부만 확인한다.
     * User 파트에서 role 체계 도입 예정 — 도입 전까지 프론트 미노출.
     */
    @Deprecated
    @Operation(summary = "[관리자 전용 예정] 캐릭터 생성",
            description = "⚠️ 관리자 권한 체계 도입 전까지 임시로 로그인 사용자 전체에게 열려 있습니다. 프론트 연동 금지.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 응답 본문 코드(CHARACTER201_1)와 실제 HTTP 상태를 201로 일치시킨다
    public ApiResponse<CharacterCreateResponse> create(@Valid @RequestBody CharacterRequest request) {
        return ApiResponse.onSuccess(InvitationSuccessCode.CHARACTER_CREATE_OK, characterService.create(request));
    }

    /**
     * 캐릭터 수정.
     *
     * @deprecated 관리자 권한 체계가 아직 없어 로그인 여부만 확인한다.
     * User 파트에서 role 체계 도입 예정 — 도입 전까지 프론트 미노출.
     */
    @Deprecated
    @Operation(summary = "[관리자 전용 예정] 캐릭터 수정",
            description = "⚠️ 관리자 권한 체계 도입 전까지 임시로 로그인 사용자 전체에게 열려 있습니다. 프론트 연동 금지.")
    @PutMapping("/{id}")
    public ApiResponse<CharacterResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody CharacterRequest request) {
        return ApiResponse.onSuccess(InvitationSuccessCode.CHARACTER_UPDATE_OK, characterService.update(id, request));
    }

    /**
     * 캐릭터 삭제 — soft delete (엔티티 CharacterEntity.delete() 주석 참고).
     *
     * @deprecated 관리자 권한 체계가 아직 없어 로그인 여부만 확인한다.
     * User 파트에서 role 체계 도입 예정 — 도입 전까지 프론트 미노출.
     */
    @Deprecated
    @Operation(summary = "[관리자 전용 예정] 캐릭터 삭제",
            description = "⚠️ 관리자 권한 체계 도입 전까지 임시로 로그인 사용자 전체에게 열려 있습니다. 프론트 연동 금지. (Soft Delete)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        characterService.delete(id);
        return ApiResponse.onSuccess(InvitationSuccessCode.CHARACTER_DELETE_OK, null);
    }
}
