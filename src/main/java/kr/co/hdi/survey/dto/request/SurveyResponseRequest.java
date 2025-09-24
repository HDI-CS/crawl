package kr.co.hdi.survey.dto.request;

public record SurveyResponseRequest(
        // 정량 평가
        Integer index,
        Integer response,

        // 정성 평가
        String textResponse
) {
}
