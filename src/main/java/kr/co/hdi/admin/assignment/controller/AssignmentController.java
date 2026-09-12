package kr.co.hdi.admin.assignment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.hdi.admin.assignment.dto.request.AssignmentDataRequest;
import kr.co.hdi.admin.assignment.dto.response.AssignmentImportResultResponse;
import kr.co.hdi.admin.assignment.dto.response.AssignmentResponse;
import kr.co.hdi.admin.assignment.service.AssignmentService;
import kr.co.hdi.admin.assignment.service.AssignmentServiceResolver;
import kr.co.hdi.admin.data.dto.request.DataIdsRequest;
import kr.co.hdi.admin.survey.dto.response.SurveyResponse;
import kr.co.hdi.admin.user.dto.response.ExpertNameResponse;
import kr.co.hdi.domain.user.entity.UserType;
import kr.co.hdi.domain.year.enums.DomainType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/{type}/assignment")
@Tag(name = "데이터셋 할당", description = "데이터셋 할당 관리 API")
public class AssignmentController {

    private final AssignmentServiceResolver resolver;

    @GetMapping("/search")
    @Operation(summary = "평가에 참여할 전문가 후보 검색")
    public ResponseEntity<List<ExpertNameResponse>> searchExpertByName(
            @PathVariable UserType type,
            @RequestParam String q) {

        AssignmentService service = resolver.resolve(type.toDomainType());
        List<ExpertNameResponse> responses = service.searchExpertByName(type, q);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/all")
    @Operation(summary = "전체 매칭 폴더 조회")
    public ResponseEntity<List<SurveyResponse>> getAssessmentList(
            @PathVariable DomainType type) {

        AssignmentService service = resolver.resolve(type);
        List<SurveyResponse> responses = service.getAssignmentYearRoundList(type);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/assessment/{assessmentRoundId}")
    @Operation(summary = "해당 차수의 데이터셋 매칭 전체 조회")
    public ResponseEntity<List<AssignmentResponse>> getDatasetAssignment(
            @PathVariable DomainType type,
            @PathVariable Long assessmentRoundId,
            @RequestParam(required = false) String q) {

        AssignmentService service = resolver.resolve(type);
        List<AssignmentResponse> responses = service.getDatasetAssignment(assessmentRoundId, q);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/assessment/{assessmentRoundId}/members/{memberId}")
    @Operation(summary = "데이터셋 매칭 전문가별 조회")
    public ResponseEntity<AssignmentResponse> getDatasetAssignmentByMember(
            @PathVariable DomainType type,
            @PathVariable Long assessmentRoundId,
            @PathVariable Long memberId
    ) {

        AssignmentService service = resolver.resolve(type);
        AssignmentResponse responses = service.getDatasetAssignmentByUser(assessmentRoundId, memberId);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @PutMapping("/assessment/{assessmentRoundId}/members/{memberId}")
    @Operation(summary = "데이터셋 매칭 수정")
    public ResponseEntity<Void> updateDatasetAssignment(
            @PathVariable DomainType type,
            @PathVariable Long assessmentRoundId,
            @PathVariable Long memberId,
            @RequestBody DataIdsRequest request
            ) {

        AssignmentService service = resolver.resolve(type);
        service.updateDatasetAssignment(assessmentRoundId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assessment/{assessmentRoundId}")
    @Operation(summary = "전문가와 데이터셋 매칭 등록")
    public ResponseEntity<Void> createDatasetAssignment(
            @PathVariable DomainType type,
            @PathVariable Long assessmentRoundId,
            @RequestBody AssignmentDataRequest request) {

        AssignmentService service = resolver.resolve(type);
        service.createDatasetAssignment(assessmentRoundId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/assessment/{assessmentRoundId}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "전문가-데이터셋 매칭 엑셀 업로드")
    public ResponseEntity<AssignmentImportResultResponse> importDatasetAssignment(
            @PathVariable DomainType type,
            @PathVariable Long assessmentRoundId,
            @RequestParam("file") MultipartFile file) {

        AssignmentService service = resolver.resolve(type);
        AssignmentImportResultResponse response = service.importDatasetAssignmentExcel(assessmentRoundId, file);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/assessment/{assessmentRoundId}/export")
    @Operation(summary = "전문가와 데이터셋 매칭 엑셀 다운로드")
    public ResponseEntity<Resource> exportDatasetAssignment(
            @PathVariable DomainType type,
            @PathVariable Long assessmentRoundId
    ) {

        AssignmentService service = resolver.resolve(type);

        byte[] bytes = service.exportDataAssignments(assessmentRoundId);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        String filename = "expert_dataset_assignment.xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentLength(bytes.length)
                .body(resource);
    }
}
