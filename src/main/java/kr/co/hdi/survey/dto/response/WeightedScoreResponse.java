package kr.co.hdi.survey.dto.response;

import kr.co.hdi.survey.domain.DatasetCategory;

public record WeightedScoreResponse(

        Long id,
        DatasetCategory category,
        int score1,
        int score2,
        int score3,
        int score4,
        int score5,
        int score6,
        int score7,
        int score8
) {
}
