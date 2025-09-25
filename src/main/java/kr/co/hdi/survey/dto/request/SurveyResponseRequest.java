package kr.co.hdi.survey.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
정량 평가, 정성 평가 응답 제출 DTO

- 정량 평가: { index, response, null }
- 정성 평가: { null, null, textResponse }
""")
public record SurveyResponseRequest(
        // 정량 평가
        @Schema(description = "문항 번호 (몇 번째 질문인지)")
        Integer index,

        @Schema(description = "정량 평가 (점수 1,2,3,4,5)")
        Integer response,

        // 정성 평가
        @Schema(description = "정성 평가 (서술형 답변)")
        String textResponse
) {
}
