package kr.co.hdi.admin.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.hdi.admin.user.dto.request.ExpertInfoRequest;
import kr.co.hdi.admin.user.dto.request.ExpertInfoUpdateRequest;
import kr.co.hdi.admin.user.dto.response.ExpertInfoResponse;
import kr.co.hdi.admin.user.service.ExpertUserService;
import kr.co.hdi.domain.user.entity.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/{type}/members")
@Tag(name = "전문가 인적사항", description = "전문가 인적사항 관리 API")
public class ExpertUserController {

    private final ExpertUserService expertUserService;

    @GetMapping
    @Operation(summary = "전문가 리스트 조회")
    public ResponseEntity<List<ExpertInfoResponse>> getExpertInfo(@PathVariable UserType type) {

        List<ExpertInfoResponse> responses = expertUserService.getExpertInfo(type);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }
    @PutMapping("/{memberId}")
    @Operation(summary = "전문가 인적사항 수정")
    public ResponseEntity<Void> updateExpertInfo(
            @PathVariable UserType type,
            @PathVariable Long memberId,
            @RequestBody ExpertInfoUpdateRequest request) {

        expertUserService.updateExpertInfo(request, memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "전문가 인적사항 등록")
    public ResponseEntity<Void> registerExpert(
            @PathVariable UserType type,
            @RequestBody ExpertInfoRequest request) {

        expertUserService.registerExpert(type, request);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/search")
    @Operation(summary = "전문가 페이지에서 검색")
    public ResponseEntity<List<ExpertInfoResponse>> searchExpert(
            @PathVariable UserType type,
            @RequestParam String q
    ) {

        List<ExpertInfoResponse> responses = expertUserService.searchExpert(type, q);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/export")
    @Operation(summary = "전문가 인적사항 엑셀 다운로드")
    public ResponseEntity<Resource> exportExpertInfo(
            @PathVariable UserType type) {

        byte[] bytes = expertUserService.exportExpertInfo(type);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        String filename = "expert_information.xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentLength(bytes.length)
                .body(resource);
    }
}
