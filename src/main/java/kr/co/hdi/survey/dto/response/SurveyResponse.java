package kr.co.hdi.survey.dto.response;

public record SurveyResponse(
        Integer index,
        String survey,
        Integer response
) {
}
