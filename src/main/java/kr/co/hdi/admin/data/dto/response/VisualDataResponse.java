package kr.co.hdi.admin.data.dto.response;

import kr.co.hdi.domain.data.entity.VisualData;

public record VisualDataResponse(
        Long id,
        String code,
        String name,
        String sectorCategory,
        String mainProductCategory,
        String mainProduct,
        String target,
        String referenceUrl,

        // 2026
        String title,
        String country,
        String clientName,
        String contentType,
        String visualType,
        String designDescription,
        String originalDescription,
        String releaseYear,

        String logoImage
) {

    public static VisualDataResponse from(VisualData v, String image) {
        return new VisualDataResponse(
                v.getId(),
                v.getBrandCode(),
                v.getBrandName(),
                v.getSectorCategory(),
                v.getMainProductCategory(),
                v.getMainProduct(),
                v.getTarget(),
                v.getReferenceUrl(),
                // 2026
                v.getTitle(),
                v.getCountry(),
                v.getClientName(),
                v.getContentType(),
                v.getVisualType(),
                v.getDesignDescription(),
                v.getOriginalDescription(),
                v.getReleaseYear(),

                image
        );
    }
}
