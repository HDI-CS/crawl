package kr.co.hdi.survey.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import kr.co.hdi.survey.dto.request.SurveyResponseRequest;
import kr.co.hdi.survey.dto.response.ProductSurveyDataResponse;
import kr.co.hdi.survey.dto.response.ProductSurveyDetailResponse;
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
public class ProductSurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "유저에게 할당된 제품 설문 목록 조회")
    @GetMapping("/product")
    public ResponseEntity<List<ProductSurveyDataResponse>> getSurveys(
            @Parameter(hidden = true) @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        log.debug("Session userId: {}", userId);

        List<ProductSurveyDataResponse> response = surveyService.getAllProductSurveys(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "제품 설문 상세 조회 (설문하러가기 누를때)")
    @GetMapping("/product/{productResponseId}")
    public ResponseEntity<ProductSurveyDetailResponse> getProductSurveyDetail(
            @PathVariable Long productResponseId
    ) {
        ProductSurveyDetailResponse response = surveyService.getProductSurveyDetail(productResponseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "제품 설문 응답 한개 저장")
    @PostMapping("/product/{productResponseId}")
    public ResponseEntity<Void> saveProductSurveyResponse(
            @PathVariable Long productResponseId,
            @RequestBody SurveyResponseRequest request,
            @Parameter(hidden = true) @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        surveyService.saveProductSurveyResponse(productResponseId, request, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "제출")
    @PostMapping("/product/{productResponseId}/submit")
    public ResponseEntity<Void> submitProductSurvey(
            @PathVariable Long productResponseId,
            @Parameter(hidden = true) @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        surveyService.setProductResponseStatusDone(productResponseId, userId);
        return ResponseEntity.ok().build();
    }

}
