package kr.co.hdi.crawl.dto;

public record ProductRequestDto(
        String productName,
        String modelName,
        String price,
        String material,
        String size,
        String weight,
        String referenceUrl,
        String frontImageUrl,
        String sideImageUrl,
        String backImageUrl
) {
}
