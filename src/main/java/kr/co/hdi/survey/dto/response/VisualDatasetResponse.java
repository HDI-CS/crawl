package kr.co.hdi.survey.dto.response;

import kr.co.hdi.domain.data.entity.VisualData;
import kr.co.hdi.domain.data.enums.VisualDataCategory;

public record VisualDatasetResponse(

        String name,
        String id,
        String sectorCategory,
        String mainProductCategory,
        String mainProduct,
        String target,
        String referenceUrl,
        String image,

        // 2026
        String title,
        String country,
        String clientName,
        String contentType,
        String visualType,
        String releaseYear,
        String designDescription,
        String originalDescription,
        VisualDataCategory visualDataCategory

) {
    public static VisualDatasetResponse fromEntity(VisualData data, String visualDataImage) {
        return new VisualDatasetResponse(
                data.getBrandName(),
                data.getBrandCode(),
                data.getSectorCategory(),
                data.getMainProductCategory(),
                data.getMainProduct(),
                data.getTarget(),
                data.getReferenceUrl(),
                visualDataImage,

                data.getTitle(),
                data.getCountry(),
                data.getClientName(),
                data.getContentType(),
                data.getVisualType(),
                data.getReleaseYear(),
                data.getDesignDescription(),
                data.getOriginalDescription(),
                data.getVisualDataCategory()

        );
    }
}
