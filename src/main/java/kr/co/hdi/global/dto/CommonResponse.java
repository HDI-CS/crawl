package kr.co.hdi.global.dto;

import lombok.Builder;

public record CommonResponse<T>(
        int status,
        String message,
        T data
) {

    @Builder
    public CommonResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

}
