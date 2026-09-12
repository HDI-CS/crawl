package kr.co.hdi.admin.assignment.exception;

import kr.co.hdi.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AssignmentErrorCode implements ErrorCode {

    INVALID_DOMAIN_TYPE(HttpStatus.BAD_REQUEST, "도메인 타입이 유효하지 않습니다."),
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 데이터입니다."),
    ASSESSMENT_ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 차수입니다."),
    USER_NOT_PARTICIPATED_IN_ASSESSMENT_ROUND(HttpStatus.BAD_REQUEST, "해당 차수에 참여하지 않은 사용자입니다."),
    INVALID_EXCEL_FORMAT(HttpStatus.BAD_REQUEST, "엑셀 양식이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
