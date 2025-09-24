package kr.co.hdi.survey.dto.response;

public record BrandSurveyDetailResponse(
        BrandDatasetResponse brandDatasetResponse,
        BrandSurveyResponse brandSurveyResponse
) {
}
