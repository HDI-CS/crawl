package kr.co.hdi.admin.data.dto.request;

import kr.co.hdi.domain.data.enums.VisualDataCategory;

public record VisualDataRequest(
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


        String originalLogoImage,
        VisualDataCategory visualDataCategory
) {
}
