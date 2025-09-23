package kr.co.hdi.survey.controller;

import kr.co.hdi.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController
@RequestMapping("/api/survey")
@Slf4j
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @GetMapping("/product")
    public ResponseEntity<List<ProductSurveyResponse>> getSurveys(
            @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        List<ProductSurveyResponse> response = surveyService.getAllProductSurveys(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/brand")
    public ResponseEntity<List<BrandSurveyResponse>> getBrandSurveys(
            @SessionAttribute(name = "userId", required = true) Long userId
    ) {
        List<BrandSurveyResponse> response = surveyService.getAllBrandSurveys(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
