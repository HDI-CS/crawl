package kr.co.hdi.survey.dto.response;

import kr.co.hdi.survey.domain.ResponseStatus;

public record ProductSurveyDataResponse(

        String name,
        String image,
        ResponseStatus responseStatus,
        Long responseId
) {
}
