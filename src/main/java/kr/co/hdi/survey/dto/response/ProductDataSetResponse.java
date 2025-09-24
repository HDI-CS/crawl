package kr.co.hdi.survey.dto.response;

import kr.co.hdi.crawl.domain.Product;

public record ProductDataSetResponse(
        String id,
        String productName,
        String companyName,
        String modelName,
        String price,
        String material,
        String size,
        String weight,
        String referenceUrl,
        String registeredAt,
        String productPath,
        String productTypeName
) {

    public static ProductDataSetResponse from(Product product) {
        return new ProductDataSetResponse(
                product.getId().toString(),
                product.getProductName(),
                product.getCompanyName(),
                product.getModelName(),
                product.getPrice(),
                product.getMaterial(),
                product.getSize(),
                product.getWeight(),
                product.getReferenceUrl(),
                product.getRegisteredAt(),
                product.getProductPath(),
                product.getProductTypeName()
        );
    }

}
