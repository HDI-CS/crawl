package kr.co.hdi.global.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.hdi.global.dto.CommonResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResponseInterceptor implements ResponseBodyAdvice {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        if (body instanceof CommonResponse) {
            return body;
        }

        int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();

        // 204인 경우 예외 처리
        if (status == HttpStatus.NO_CONTENT.value()) {
            return body;
        }

        // swagger 제외
        String path = request.getURI().getPath();

        if (path.startsWith("/actuator") ||
                path.contains("swagger") ||
                path.contains("api-docs") ||
                path.contains("webjars") ||
                path.startsWith("/internal")) {
            return body;
        }
        // 조건부 메시지 처리: 2xx -> "Success", 그 외 -> "Error"
        String message = (status >= 200 && status < 300) ? "OK" : "Error";

        CommonResponse<Object> commonResponse = CommonResponse.builder()
                .status(status)
                .message(message)
                .data(body)
                .build();

        // 응답을 String으로 내는 경우 따로 예외처리
        if (body instanceof String) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.writeValueAsString(commonResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("String response conversion error", e);
            }
        }
        return commonResponse;
    }

}
