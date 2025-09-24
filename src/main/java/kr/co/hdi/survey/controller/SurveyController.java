package kr.co.hdi.survey.controller;

import kr.co.hdi.survey.dto.request.SurveyResponseRequest;
import kr.co.hdi.survey.dto.request.WeightedScoreRequest;
import kr.co.hdi.survey.dto.response.BrandSurveyDetailResponse;
import kr.co.hdi.survey.dto.response.SurveyDataResponse;
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
public class SurveyController {

    private final SurveyService surveyService;

    @GetMapping("/product")
    public ResponseEntity<List<SurveyDataResponse>> getSurveys(
            @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        List<SurveyDataResponse> response = surveyService.getAllProductSurveys(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/brand")
    public ResponseEntity<List<SurveyDataResponse>> getBrandSurveys(
            @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        List<SurveyDataResponse> response = surveyService.getAllBrandSurveys(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/brand/{brandResponseId}")
    public ResponseEntity<BrandSurveyDetailResponse> getBrandSurveyDetail(
            @PathVariable Long brandResponseId
    ) {

        BrandSurveyDetailResponse response = surveyService.getBrandSurveyDetail(brandResponseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/brand/{brandResponseId}")
    public ResponseEntity<Void> saveBrandSurveyResponse(
            @PathVariable Long brandResponseId,
            @RequestBody SurveyResponseRequest request) {

        surveyService.saveBrandSurveyResponse(brandResponseId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/brand/{brandResponseId}/submit")
    public ResponseEntity<Void> submitBrandSurvey(
            @PathVariable Long brandResponseId,
            @SessionAttribute(name = "userId", required = true) Long userId
    ) {

        surveyService.setBrandResponseStatusDone(brandResponseId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/scores/weighted")
    public ResponseEntity<Void> saveWeightedScores(
            @RequestBody List<WeightedScoreRequest> requests,
            @SessionAttribute(name = "userId", required = true) Long userId
    ) {

        surveyService.saveWeightedScores(userId, requests);
        return ResponseEntity.ok().build();
    }
}
