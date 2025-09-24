package kr.co.hdi.survey.dto.response;

public record ProductSurveyDetailResponse(
        ProductDataSetResponse productDataSetResponse,
        ProductSurveyResponse productSurveyResponse
) {
}
