package kr.co.hdi.domain.data.entity;

import jakarta.persistence.*;
import kr.co.hdi.admin.data.dto.request.IndustryDataRequest;
import kr.co.hdi.admin.data.dto.request.VisualDataRequest;
import kr.co.hdi.admin.data.exception.DataErrorCode;
import kr.co.hdi.admin.data.exception.DataException;

import kr.co.hdi.domain.data.enums.IndustryDataCategory;
import kr.co.hdi.domain.year.entity.Year;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class IndustryData extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "industry_data_id")
    private Long id;

    private String companyName;

    @Column(nullable = false)
    private String productName;

    private String modelName;
    private String price;
    private String material;
    private String size;
    private String weight;
    private String referenceUrl;

    private String registeredAt;
    private String productPath;

    private String productTypeName;

    // 2026
    private String noiseCancelling;
    private String codec;

    @Column(columnDefinition = "text")
    private String extraFeatures;

    private String controlType;
    private String waterproof;
    private String maxPlayTime;
    private String chargeTime;
    private String usage;
    private String shoppingUrl;
    private String soundOutput;
    private String connectivity;

    @Column(name = "original_id")
    private String originalId;

    @Enumerated(EnumType.STRING)
    private IndustryDataCategory industryDataCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    private Year year;

    @Column(columnDefinition = "text")
    private String detailImagePath;   // 상세 이미지 S3 Key

    @Column(columnDefinition = "text")
    private String originalDetailImagePath;

    @Column(columnDefinition = "text")
    private String frontImagePath;    // 정면 이미지 S3 Key

    @Column(columnDefinition = "text")
    private String originalFrontImagePath;

    @Column(columnDefinition = "text")
    private String sideImagePath;     // 측면 이미지 S3 Key

    @Column(columnDefinition = "text")
    private String originalSideImagePath;

    @Column(columnDefinition = "text")
    private String side2ImagePath;     // 측면 2 이미지 S3 Key

    @Column(columnDefinition = "text")
    private String originalSide2ImagePath;

    @Column(columnDefinition = "text")
    private String side3ImagePath;     // 측면 3 이미지 S3 Key

    @Column(columnDefinition = "text")
    private String originalSide3ImagePath;

    private Integer originalIdInteger;


    public void delete() {
        processDeletion();
    }

    public String deleteImage(String imageStatus) {

        if (imageStatus.equals("DETAIL")) {
            deleteDetailImage();
            return this.detailImagePath;
        }
        if (imageStatus.equals("FRONT")) {
            deleteFrontImage();
            return this.frontImagePath;
        }
        if (imageStatus.equals("SIDE")) {
            deleteSideImage();
            return this.sideImagePath;
        }
        if (imageStatus.equals("SIDE2")) {
            deleteSide2Image();
            return this.side2ImagePath;
        }
        if (imageStatus.equals("SIDE3")) {
            deleteSide3Image();
            return this.side3ImagePath;
        }
        return null;
    }

    private void deleteDetailImage() {
        this.originalDetailImagePath = null;
    }

    private void deleteFrontImage() {
        this.originalFrontImagePath = null;
    }

    private void deleteSideImage() {
        this.originalSideImagePath = null;
    }

    private void deleteSide2Image() {
        this.originalSide2ImagePath = null;
    }

    private void deleteSide3Image() {
        this.originalSide3ImagePath = null;
    }

    /*
    IndustryData 생성
     */
    public static IndustryData create(Year year, IndustryDataRequest request) {
        IndustryData i = new IndustryData();

        i.year = year;
        i.originalId = request.code();
        i.companyName = request.companyName();
        i.productName = request.productName();
        i.modelName = request.modelName();
        i.price = request.price();
        i.material = request.material();
        i.size = request.size();
        i.weight = request.weight();
        i.referenceUrl = request.referenceUrl();
        i.registeredAt = request.registeredAt();
        i.productPath = request.productPath();
        i.productTypeName = request.productTypeName();
        i.industryDataCategory = request.industryDataCategory();

        // 2026
        i.noiseCancelling = request.noiseCancelling();
        i.codec = request.codec();
        i.extraFeatures = request.extraFeatures();
        i.controlType = request.controlType();
        i.waterproof = request.waterproof();
        i.maxPlayTime = request.maxPlayTime();
        i.chargeTime = request.chargeTime();
        i.usage = request.usage();
        i.shoppingUrl = request.shoppingUrl();
        i.connectivity = request.connectivity();
        i.soundOutput = request.soundOutput();






        i.originalDetailImagePath = request.originalDetailImagePath();
        i.detailImagePath = "2026/ID/" + UUID.randomUUID();
        i.originalFrontImagePath = request.originalFrontImagePath();
        i.frontImagePath =  "2026/ID/" + UUID.randomUUID();
        i.originalSideImagePath = request.originalSideImagePath();
        i.sideImagePath =  "2026/ID/" + UUID.randomUUID();

        i.originalSide2ImagePath = request.originalSide2ImagePath();
        i.side2ImagePath = "2026/ID/" + UUID.randomUUID();

        i.originalSide3ImagePath = request.originalSide3ImagePath();
        i.side3ImagePath =  "2026/ID/" + UUID.randomUUID();

        return i;
    }

    /*
    IndustryData 복제
    id, createdAt, updatedAt, deletedAt 제외
     */

    public IndustryData duplicate() {

        IndustryData copy = new IndustryData();

        copy.year = this.year;
        copy.originalId = this.originalId;
        copy.companyName = this.companyName;
        copy.productName = this.productName;
        copy.modelName = this.modelName;
        copy.price = this.price;
        copy.material = this.material;
        copy.size = this.size;
        copy.weight = this.weight;
        copy.referenceUrl = this.referenceUrl;
        copy.registeredAt = this.registeredAt;
        copy.productPath = this.productPath;
        copy.productTypeName = this.productTypeName;
        copy.industryDataCategory = this.industryDataCategory;

        // 2026
        copy.noiseCancelling = this.noiseCancelling;
        copy.codec = this.codec;
        copy.extraFeatures = this.extraFeatures;
        copy.controlType = this.controlType;
        copy.waterproof = this.waterproof;
        copy.maxPlayTime = this.maxPlayTime;
        copy.chargeTime = this.chargeTime;
        copy.usage = this.usage;
        copy.shoppingUrl = this.shoppingUrl;
        copy.connectivity = this.connectivity;
        copy.soundOutput = this.soundOutput;


        copy.detailImagePath = "2026/ID/" + UUID.randomUUID();
        copy.frontImagePath = "2026/ID/" + UUID.randomUUID();
        copy.sideImagePath = "2026/ID/" + UUID.randomUUID();
        copy.side2ImagePath = "2026/ID/" + UUID.randomUUID();
        copy.side3ImagePath = "2026/ID/" + UUID.randomUUID();

        return copy;
    }

    /*
    IndustryData 부분 수정
     */
    public void updatePartial(IndustryDataRequest request) {

        if (request.code() != null) {
            this.originalId = request.code();
        }
        if (request.companyName() != null) {
            this.companyName = request.companyName();
        }
        if (request.productName() != null) {
            this.productName = request.productName();
        }
        if (request.modelName() != null) {
            this.modelName = request.modelName();
        }
        if (request.price() != null) {
            this.price = request.price();
        }
        if (request.material() != null) {
            this.material = request.material();
        }
        if (request.size() != null) {
            this.size = request.size();
        }
        if (request.weight() != null) {
            this.weight = request.weight();
        }
        if (request.referenceUrl() != null) {
            this.referenceUrl = request.referenceUrl();
        }
        if (request.registeredAt() != null) {
            this.registeredAt = request.registeredAt();
        }
        if (request.productPath() != null) {
            this.productPath = request.productPath();
        }
        if (request.productTypeName() != null) {
            this.productTypeName = request.productTypeName();
        }
        if (request.industryDataCategory() != null) {
            this.industryDataCategory = request.industryDataCategory();
        }
        if (request.originalDetailImagePath() != null) {
            this.originalDetailImagePath = request.originalDetailImagePath();
        }
        if (request.originalFrontImagePath() != null) {
            this.originalFrontImagePath = request.originalFrontImagePath();
        }
        if (request.originalSideImagePath() != null) {
            this.originalSideImagePath = request.originalSideImagePath();
        }
        if (request.originalSide2ImagePath() != null) {
            this.originalSide2ImagePath = request.originalSide2ImagePath();
        }
        if (request.originalSide3ImagePath() != null) {
            this.originalSide3ImagePath = request.originalSide3ImagePath();
        }

        // 2026
        if (request.noiseCancelling() != null) {
            this.noiseCancelling = request.noiseCancelling();
        }
        if (request.codec() != null) {
            this.codec = request.codec();
        }
        if (request.extraFeatures() != null) {
            this.extraFeatures = request.extraFeatures();
        }
        if (request.controlType() != null) {
            this.controlType = request.controlType();
        }
        if (request.waterproof() != null) {
            this.waterproof = request.waterproof();
        }
        if (request.maxPlayTime() != null) {
            this.maxPlayTime = request.maxPlayTime();
        }
        if (request.chargeTime() != null) {
            this.chargeTime = request.chargeTime();
        }
        if (request.usage() != null) {
            this.usage = request.usage();
        }
        if (request.shoppingUrl() != null) {
            this.shoppingUrl = request.shoppingUrl();
        }
        if (request.connectivity() != null) {
            this.connectivity = request.connectivity();
        }
        if (request.soundOutput() != null) {
            this.soundOutput = request.soundOutput();
        }
    }


    @PrePersist
    @PreUpdate
    private void syncOriginalIdInteger() {
        if (this.originalId != null) {
            try {
                this.originalIdInteger = Integer.parseInt(this.originalId);
            } catch (NumberFormatException e) {
                this.originalIdInteger = null;
            }
        }
    }


    public void updateImageKeys(
            String detailKey, String frontKey, String sideKey, String side2Key, String side3Key,
            String originalDetailName, String originalFrontName, String originalSideName,
            String originalSide2Name, String originalSide3Name
    ) {
        if (detailKey != null) {
            this.detailImagePath = detailKey;
            this.originalDetailImagePath = originalDetailName;
        }
        if (frontKey != null) {
            this.frontImagePath = frontKey;
            this.originalFrontImagePath = originalFrontName;
        }
        if (sideKey != null) {
            this.sideImagePath = sideKey;
            this.originalSideImagePath = originalSideName;
        }
        if (side2Key != null) {
            this.side2ImagePath = side2Key;
            this.originalSide2ImagePath = originalSide2Name;
        }
        if (side3Key != null) {
            this.side3ImagePath = side3Key;
            this.originalSide3ImagePath = originalSide3Name;
        }
    }

}