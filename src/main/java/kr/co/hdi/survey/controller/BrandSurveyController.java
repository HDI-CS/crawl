package kr.co.hdi.survey.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import kr.co.hdi.survey.dto.request.SurveyResponseRequest;
import kr.co.hdi.survey.dto.request.WeightedScoreRequest;
import kr.co.hdi.survey.dto.response.BrandSurveyDetailResponse;
import kr.co.hdi.survey.dto.response.ProductSurveyDataResponse;
import kr.co.hdi.survey.dto.response.WeightedScoreResponse;
import kr.co.hdi.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/survey")
@Slf4j
@RequiredArgsConstructor
public class BrandSurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "유저에게 할당된 브랜드 설문 목록 조회")
    @GetMapping("/brand")
    public ResponseEntity<List<ProductSurveyDataResponse>> getBrandSurveys(
            @Parameter(hidden = true) @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        List<ProductSurveyDataResponse> response = surveyService.getAllBrandSurveys(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "브랜드 설문 상세 조회 (설문하러가기 누를때)")
    @GetMapping("/brand/{brandResponseId}")
    public ResponseEntity<BrandSurveyDetailResponse> getBrandSurveyDetail(
            @PathVariable Long brandResponseId
    ) {

        BrandSurveyDetailResponse response = surveyService.getBrandSurveyDetail(brandResponseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "브랜드 설문 응답 한개 저장")
    @PostMapping("/brand/{brandResponseId}")
    public ResponseEntity<Void> saveBrandSurveyResponse(
            @PathVariable Long brandResponseId,
            @RequestBody SurveyResponseRequest request) {

        surveyService.saveBrandSurveyResponse(brandResponseId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "제출")
    @PostMapping("/brand/{brandResponseId}/submit")
    public ResponseEntity<Void> submitBrandSurvey(
            @PathVariable Long brandResponseId,
            @Parameter(hidden = true) @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        surveyService.setBrandResponseStatusDone(brandResponseId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "가중치 평가")
    @PatchMapping("/scores/weighted")
    public ResponseEntity<Void> saveWeightedScores(
            @RequestBody List<WeightedScoreRequest> requests,
            @Parameter(hidden = true) @SessionAttribute(name = "userId", required = true) Long userId
    ) {

        surveyService.saveWeightedScores(userId, requests);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/scores/weighted")
    public ResponseEntity<List<WeightedScoreResponse>> getWeightesScores(
            @Parameter(hidden = true) @SessionAttribute(name = "userId", required = true) Long userId
    ) {

        List<WeightedScoreResponse> response = surveyService.getWeightedResponse(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
