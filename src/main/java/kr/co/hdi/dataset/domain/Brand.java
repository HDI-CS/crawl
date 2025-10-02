package kr.co.hdi.dataset.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Brand extends BaseTimeEntityWithDeletion {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String brandCode;

    private String brandName;

    private String sectorCategory;

    private String mainProductCategory;

    private String mainProduct;

    private String target;

    @Column(columnDefinition = "text")
    private String referenceUrl;

    private String image;

    @Builder
    public Brand(String brandCode, String brandName, String sectorCategory,
                  String mainProductCategory, String mainProduct, String target, String referenceUrl, String image) {

        this.brandCode = brandCode;
        this.brandName = brandName;
        this.sectorCategory = sectorCategory;
        this.mainProductCategory = mainProductCategory;
        this.mainProduct = mainProduct;
        this.target = target;
        this.referenceUrl = referenceUrl;
        this.image = image;
    }
}
