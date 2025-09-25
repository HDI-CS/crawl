package kr.co.hdi.survey.exception;

import kr.co.hdi.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum SurveyErrorCode implements ErrorCode {

    SURVEY_NOT_FOUND(HttpStatus.NOT_FOUND, "설문 정보를 찾을 수 없습니다."),
    BRAND_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "브랜드 응답 정보를 찾을 수 없습니다."),
    INVALID_RESPONSE_INDEX(HttpStatus.BAD_REQUEST, "잘못된 문항 번호입니다."),
    INCOMPLETE_RESPONSE(HttpStatus.BAD_REQUEST, "모든 응답이 채워지지 않았습니다."),
    PRODUCT_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "상품 응답 정보를 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.BAD_REQUEST, "권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
