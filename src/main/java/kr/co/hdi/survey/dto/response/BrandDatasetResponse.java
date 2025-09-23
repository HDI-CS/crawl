package kr.co.hdi.survey.dto.response;

import kr.co.hdi.dataset.domain.Brand;

public record BrandDatasetResponse(
        String id,
        String sectorCategory,
        String mainProductCategory,
        String mainProduct,
        String target,
        String referenceUrl,
        String image
) {

    public static BrandDatasetResponse fromEntity(Brand brand) {
        return new BrandDatasetResponse(
                brand.getBrandCode(),
                brand.getSectorCategory(),
                brand.getMainProductCategory(),
                brand.getMainProduct(),
                brand.getTarget(),
                brand.getReferenceUrl(),
                brand.getImage()
        );
    }
}
