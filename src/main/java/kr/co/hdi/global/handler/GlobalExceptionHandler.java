package kr.co.hdi.global.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import kr.co.hdi.global.dto.ResponseError;
import kr.co.hdi.global.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ResponseError> handleCustomException(CustomException e,
                                                               HttpServletRequest request) {

        ResponseError responseError = ResponseError.builder()
                .messageDetail(e.getMessage())
                .errorDetail(e.getErrorCode().getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(responseError);

    }

    // 요청 본문이 없거나 변환할 수 없는 경우 (NOT NULL)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseError> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
                                                                                     HttpServletRequest request) {

        ResponseError responseError = ResponseError.builder()
                .messageDetail("요청 본문이 누락되었거나 올바르지 않습니다.")
                .errorDetail(e.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseError);
    }

    // @Valid 어노테이션으로 DTO 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseError> handleValidationExceptions(MethodArgumentNotValidException e,
                                                                    HttpServletRequest request) {
        ResponseError responseError = ResponseError.builder()
                .messageDetail("유효성 검사 실패")
                .errorDetail(e.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseError);
    }


    // Request Param, Path Variable 등의 유효성 검사 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseError> handleConstraintViolationException(ConstraintViolationException e,
                                                                            HttpServletRequest request) {

        ResponseError responseError = ResponseError.builder()
                .messageDetail("파라미터 유효성 검사 실패")
                .errorDetail(e.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseError);
    }

    @ExceptionHandler({HttpSessionRequiredException.class, MissingRequestCookieException.class})
    public ResponseEntity<ResponseError> handleSessionRequiredException(Exception e,
                                                                        HttpServletRequest request) {
        // 기존의 ResponseError DTO 형식에 맞춰 에러 응답을 생성합니다.
        ResponseError responseError = ResponseError.builder()
                .messageDetail("로그인이 필요한 서비스입니다.") // 클라이언트에게 보여줄 명확한 메시지
                .errorDetail("세션이 없거나 유효하지 않습니다.") // 개발자를 위한 구체적인 에러 내용
                .path(request.getRequestURI())
                .build();

        // 500 에러 대신, 의미에 맞는 401 Unauthorized 상태 코드를 반환합니다.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseError);
    }


}