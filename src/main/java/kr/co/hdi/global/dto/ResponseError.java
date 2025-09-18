package kr.co.hdi.global.dto;

import lombok.Builder;

public record ResponseError(
        String path,
        String messageDetail,
        String errorDetail
) {

    @Builder
    public ResponseError(String path, String messageDetail, String errorDetail) {
        this.path = path;
        this.messageDetail = messageDetail;
        this.errorDetail = errorDetail;
    }
}
