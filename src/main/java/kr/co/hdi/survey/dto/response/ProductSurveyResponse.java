package kr.co.hdi.survey.dto.response;

import kr.co.hdi.survey.domain.ProductResponse;
import kr.co.hdi.survey.domain.ProductSurvey;

import java.util.List;

public record ProductSurveyResponse(
        List<SurveyResponse> surveyResponses,
        TextSurveyResponse textSurveyResponse
) {
    public static ProductSurveyResponse from(ProductSurvey productSurvey, ProductResponse productResponse) {
        List<SurveyResponse> surveyResponses = List.of(
                new SurveyResponse(1, productSurvey.getSurvey1(), productResponse.getResponse1()),
                new SurveyResponse(2, productSurvey.getSurvey2(), productResponse.getResponse2()),
                new SurveyResponse(3, productSurvey.getSurvey3(), productResponse.getResponse3()),
                new SurveyResponse(4, productSurvey.getSurvey4(), productResponse.getResponse4()),
                new SurveyResponse(5, productSurvey.getSurvey5(), productResponse.getResponse5()),
                new SurveyResponse(6, productSurvey.getSurvey6(), productResponse.getResponse6()),
                new SurveyResponse(7, productSurvey.getSurvey7(), productResponse.getResponse7()),
                new SurveyResponse(8, productSurvey.getSurvey8(), productResponse.getResponse8()),
                new SurveyResponse(9, productSurvey.getSurvey9(), productResponse.getResponse9()),
                new SurveyResponse(10, productSurvey.getSurvey10(), productResponse.getResponse10()),
                new SurveyResponse(11, productSurvey.getSurvey11(), productResponse.getResponse11()),
                new SurveyResponse(12, productSurvey.getSurvey12(), productResponse.getResponse12()),
                new SurveyResponse(13, productSurvey.getSurvey13(), productResponse.getResponse13()),
                new SurveyResponse(14, productSurvey.getSurvey14(), productResponse.getResponse14()),
                new SurveyResponse(15, productSurvey.getSurvey15(), productResponse.getResponse15()),
                new SurveyResponse(16, productSurvey.getSurvey16(), productResponse.getResponse16()),
                new SurveyResponse(17, productSurvey.getSurvey17(), productResponse.getResponse17()),
                new SurveyResponse(18, productSurvey.getSurvey18(), productResponse.getResponse18()),
                new SurveyResponse(19, productSurvey.getSurvey19(), productResponse.getResponse19()),
                new SurveyResponse(20, productSurvey.getSurvey20(), productResponse.getResponse20()),
                new SurveyResponse(21, productSurvey.getSurvey21(), productResponse.getResponse21()),
                new SurveyResponse(22, productSurvey.getSurvey22(), productResponse.getResponse22()),
                new SurveyResponse(23, productSurvey.getSurvey23(), productResponse.getResponse23()),
                new SurveyResponse(24, productSurvey.getSurvey24(), productResponse.getResponse24()),
                new SurveyResponse(25, productSurvey.getSurvey25(), productResponse.getResponse25()),
                new SurveyResponse(26, productSurvey.getSurvey26(), productResponse.getResponse26()),
                new SurveyResponse(27, productSurvey.getSurvey27(), productResponse.getResponse27()),
                new SurveyResponse(28, productSurvey.getSurvey28(), productResponse.getResponse28()),
                new SurveyResponse(29, productSurvey.getSurvey29(), productResponse.getResponse29()),
                new SurveyResponse(30, productSurvey.getSurvey30(), productResponse.getResponse30()),
                new SurveyResponse(31, productSurvey.getSurvey31(), productResponse.getResponse31()),
                new SurveyResponse(32, productSurvey.getSurvey32(), productResponse.getResponse32()),
                new SurveyResponse(33, productSurvey.getSurvey33(), productResponse.getResponse33()),
                new SurveyResponse(34, productSurvey.getSurvey34(), productResponse.getResponse34()),
                new SurveyResponse(35, productSurvey.getSurvey35(), productResponse.getResponse35()),
                new SurveyResponse(36, productSurvey.getSurvey36(), productResponse.getResponse36()),
                new SurveyResponse(37, productSurvey.getSurvey37(), productResponse.getResponse37()),
                new SurveyResponse(38, productSurvey.getSurvey38(), productResponse.getResponse38()),
                new SurveyResponse(39, productSurvey.getSurvey39(), productResponse.getResponse39()),
                new SurveyResponse(40, productSurvey.getSurvey40(), productResponse.getResponse40()),
                new SurveyResponse(41, productSurvey.getSurvey41(), productResponse.getResponse41())
                );
        TextSurveyResponse textSurveyResponse = new TextSurveyResponse(productSurvey.getTextSurvey(), productResponse.getTextResponse());
        return new ProductSurveyResponse(surveyResponses, textSurveyResponse);
    }
}
