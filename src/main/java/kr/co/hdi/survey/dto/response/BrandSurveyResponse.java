package kr.co.hdi.survey.dto.response;

import kr.co.hdi.survey.domain.BrandResponse;
import kr.co.hdi.survey.domain.BrandSurvey;

import java.util.List;

public record BrandSurveyResponse(
        String dataId,
        List<SurveyResponse> response,
        TextSurveyResponse textResponse
) {

    public static BrandSurveyResponse fromEntity(String dataId, BrandSurvey survey, BrandResponse br) {
        List<SurveyResponse> responses = List.of(
                new SurveyResponse(1, survey.getSurvey1(), br.getResponse1()),
                new SurveyResponse(2, survey.getSurvey2(), br.getResponse2()),
                new SurveyResponse(3, survey.getSurvey3(), br.getResponse3()),
                new SurveyResponse(4, survey.getSurvey4(), br.getResponse4()),
                new SurveyResponse(5, survey.getSurvey5(), br.getResponse5()),
                new SurveyResponse(6, survey.getSurvey6(), br.getResponse6()),
                new SurveyResponse(7, survey.getSurvey7(), br.getResponse7()),
                new SurveyResponse(8, survey.getSurvey8(), br.getResponse8()),
                new SurveyResponse(9, survey.getSurvey9(), br.getResponse9()),
                new SurveyResponse(10, survey.getSurvey10(), br.getResponse10()),
                new SurveyResponse(11, survey.getSurvey11(), br.getResponse11()),
                new SurveyResponse(12, survey.getSurvey12(), br.getResponse12()),
                new SurveyResponse(13, survey.getSurvey13(), br.getResponse13()),
                new SurveyResponse(14, survey.getSurvey14(), br.getResponse14()),
                new SurveyResponse(15, survey.getSurvey15(), br.getResponse15()),
                new SurveyResponse(16, survey.getSurvey16(), br.getResponse16()),
                new SurveyResponse(17, survey.getSurvey17(), br.getResponse17()),
                new SurveyResponse(18, survey.getSurvey18(), br.getResponse18()),
                new SurveyResponse(19, survey.getSurvey19(), br.getResponse19()),
                new SurveyResponse(20, survey.getSurvey20(), br.getResponse20()),
                new SurveyResponse(21, survey.getSurvey21(), br.getResponse21()),
                new SurveyResponse(22, survey.getSurvey22(), br.getResponse22()),
                new SurveyResponse(23, survey.getSurvey23(), br.getResponse23()),
                new SurveyResponse(24, survey.getSurvey24(), br.getResponse24()),
                new SurveyResponse(25, survey.getSurvey25(), br.getResponse25()),
                new SurveyResponse(26, survey.getSurvey26(), br.getResponse26()),
                new SurveyResponse(27, survey.getSurvey27(), br.getResponse27()),
                new SurveyResponse(28, survey.getSurvey28(), br.getResponse28()),
                new SurveyResponse(29, survey.getSurvey29(), br.getResponse29()),
                new SurveyResponse(30, survey.getSurvey30(), br.getResponse30())
        );
        TextSurveyResponse textSurveyResponse
                = new TextSurveyResponse(survey.getTextSurvey(), br.getTextResponse());
        return new BrandSurveyResponse(dataId, responses, textSurveyResponse);
    }
}
