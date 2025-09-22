package kr.co.hdi.dataset.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Brand extends BaseTimeEntityWithDeletion {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Enumerated(STRING)
    private DataType brandType;

    private int brandCodeInt;

    private String brandCode;

    private String brandName;

    private String sectorCategory;

    private String mainProductCategory;

    private String mainProduct;

    private String target;

    private String referenceUrl;

    private String image;

    @Builder(access = PRIVATE)
    private Brand(DataType brandType, int brandCodeInt, String brandCode, String brandName, String sectorCategory,
                  String mainProductCategory, String mainProduct, String target, String referenceUrl, String image) {

        this.brandType = brandType;
        this.brandCodeInt = brandCodeInt;
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
