package kr.co.hdi.survey.dto.response;

import kr.co.hdi.crawl.domain.Product;
import kr.co.hdi.crawl.domain.ProductImage;

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
        String productTypeName,

        String detailImagePath,
        String frontImagePath,
        String sideImagePath
) {

    public static ProductDataSetResponse from(Product product, ProductImage image) {
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
                product.getProductTypeName(),
                image.getDetailPath(),
                image.getFrontPath(),
                image.getSidePath()
        );
    }

}
