package kr.co.hdi.domain.data.entity;

import jakarta.persistence.*;
import kr.co.hdi.admin.data.dto.request.VisualDataRequest;
import kr.co.hdi.domain.data.enums.VisualDataCategory;
import kr.co.hdi.domain.year.entity.Year;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class VisualData extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "visual_data_id")
    private Long id;

    private String brandCode;

    private String brandName;

    private String sectorCategory;

    private String mainProductCategory;

    private String mainProduct;

    private String target;

    private String title;
    private String country;
    private String clientName;
    private String contentType;
    private String visualType;

    @Column(columnDefinition = "text")
    private String designDescription;

    @Column(columnDefinition = "text")
    private String originalDescription;

    private String releaseYear;

    @Column(columnDefinition = "text")
    private String referenceUrl;

    @Enumerated(EnumType.STRING)
    private VisualDataCategory visualDataCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    private Year year;

    @Column(columnDefinition = "text")
    private String logoImage;    // S3 key

    @Column(columnDefinition = "text")
    private String originalLogoImage;   // image 원본 이름

    private Integer brandCodeInteger;

    public void delete() {
        processDeletion();
    }

    public void deleteImage() {
        this.originalLogoImage = null;
    }

    /*
    VisualData 생성
    Year는 Service 계층에서 주입된다.
     */
    public static VisualData create(Year year, VisualDataRequest request) {
        VisualData v = new VisualData();

        v.year = year;
        v.brandCode = request.code();
        v.brandName = request.name();
        v.sectorCategory = request.sectorCategory();
        v.mainProductCategory = request.mainProductCategory();
        v.mainProduct = request.mainProduct();
        v.target = request.target();
        v.referenceUrl = request.referenceUrl();
        v.visualDataCategory = request.visualDataCategory();

        v.title = request.title();
        v.country = request.country();
        v.clientName = request.clientName();
        v.contentType = request.contentType();
        v.visualType = request.visualType();
        v.designDescription = request.designDescription();
        v.originalDescription = request.originalDescription();
        v.releaseYear = request.releaseYear();

        v.originalLogoImage = request.originalLogoImage();
        v.logoImage = "2026/VI/" + UUID.randomUUID();

        return v;
    }

    /*
    VisualData 복제
    id, createdAt, updatedAt, deletedAt 제외
     */
    public VisualData duplicate() {

        VisualData copy = new VisualData();

        copy.brandCode = this.brandCode;
        copy.brandName = this.brandName;
        copy.sectorCategory = this.sectorCategory;
        copy.mainProductCategory = this.mainProductCategory;
        copy.mainProduct = this.mainProduct;
        copy.target = this.target;
        copy.referenceUrl = this.referenceUrl;

        // 2026
        copy.title = this.title;
        copy.country = this.country;
        copy.clientName = this.clientName;
        copy.contentType = this.contentType;
        copy.visualType = this.visualType;
        copy.designDescription = this.designDescription;
        copy.originalDescription = this.originalDescription;
        copy.releaseYear = this.releaseYear;

        copy.visualDataCategory = this.visualDataCategory;
        copy.year = this.year;
        copy.logoImage = "2026/VI/" + UUID.randomUUID();

        return copy;
    }

    /*
    VisualData 부분 수정
     */
    public void updatePartial(VisualDataRequest request) {

        if (request.code() != null) {
            this.brandCode = request.code();
        }
        if (request.name() != null) {
            this.brandName = request.name();
        }
        if (request.sectorCategory() != null) {
            this.sectorCategory = request.sectorCategory();
        }
        if (request.mainProductCategory() != null) {
            this.mainProductCategory = request.mainProductCategory();
        }
        if (request.mainProduct() != null) {
            this.mainProduct = request.mainProduct();
        }
        if (request.target() != null) {
            this.target = request.target();
        }
        if (request.referenceUrl() != null) {
            this.referenceUrl = request.referenceUrl();
        }

        // 2026
        if (request.title() != null) {
            this.title = request.title();
        }
        if (request.country() != null) {
            this.country = request.country();
        }
        if (request.clientName() != null) {
            this.clientName = request.clientName();
        }
        if (request.contentType() != null) {
            this.contentType = request.contentType();
        }
        if (request.visualType() != null) {
            this.visualType = request.visualType();
        }
        if (request.designDescription() != null) {
            this.designDescription = request.designDescription();
        }
        if (request.originalDescription() != null) {
            this.originalDescription = request.originalDescription();
        }
        if (request.releaseYear() != null) {
            this.releaseYear = request.releaseYear();
        }

        if (request.visualDataCategory() != null) {
            this.visualDataCategory = request.visualDataCategory();
        }
        if (request.originalLogoImage() != null) {
            this.originalLogoImage = request.originalLogoImage();
        }
    }

    @PrePersist
    @PreUpdate
    private void syncBrandCodeInteger() {
        if (this.brandCode != null) {
            try {
                this.brandCodeInteger = Integer.parseInt(this.brandCode);
            } catch (NumberFormatException e) {
                this.brandCodeInteger = null;
            }
        }
    }



}