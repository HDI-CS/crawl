package kr.co.hdi.survey.exception;

import kr.co.hdi.global.exception.CustomException;

public class SurveyException extends CustomException {

    public SurveyException(SurveyErrorCode errorCode) {
        super(errorCode);
    }

    public SurveyException(SurveyErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
