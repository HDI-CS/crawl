package kr.co.hdi.survey.domain;

import jakarta.persistence.*;
import kr.co.hdi.dataset.domain.Brand;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import kr.co.hdi.survey.exception.SurveyErrorCode;
import kr.co.hdi.survey.exception.SurveyException;
import kr.co.hdi.user.domain.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class BrandResponse extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Enumerated(EnumType.STRING)
    private ResponseStatus responseStatus;

    private Integer response1;

    private Integer response2;

    private Integer response3;

    private Integer response4;

    private Integer response5;

    private Integer response6;

    private Integer response7;

    private Integer response8;

    private Integer response9;

    private Integer response10;

    private Integer response11;

    private Integer response12;

    private Integer response13;

    private Integer response14;

    private Integer response15;

    private Integer response16;

    private Integer response17;

    private Integer response18;

    private Integer response19;

    private Integer response20;

    private Integer response21;

    private Integer response22;

    private Integer response23;

    private Integer response24;

    private Integer response25;

    private Integer response26;

    private Integer response27;

    private Integer response28;

    private Integer response29;

    private Integer response30;

    private String textResponse;

    public static BrandResponse createBrandResponse(UserEntity user, Brand brand) {

        return BrandResponse.builder()
                .user(user)
                .brand(brand)
                .responseStatus(ResponseStatus.NOT_STARTED)
                .build();
    }

    public void updateResponse(int index, Integer value) {
        switch (index) {
            case 1 -> this.response1 = value;
            case 2 -> this.response2 = value;
            case 3 -> this.response3 = value;
            case 4 -> this.response4 = value;
            case 5 -> this.response5 = value;
            case 6 -> this.response6 = value;
            case 7 -> this.response7 = value;
            case 8 -> this.response8 = value;
            case 9 -> this.response9 = value;
            case 10 -> this.response10 = value;
            case 11 -> this.response11 = value;
            case 12 -> this.response12 = value;
            case 13 -> this.response13 = value;
            case 14 -> this.response14 = value;
            case 15 -> this.response15 = value;
            case 16 -> this.response16 = value;
            case 17 -> this.response17 = value;
            case 18 -> this.response18 = value;
            case 19 -> this.response19 = value;
            case 20 -> this.response20 = value;
            case 21 -> this.response21 = value;
            case 22 -> this.response22 = value;
            case 23 -> this.response23 = value;
            case 24 -> this.response24 = value;
            case 25 -> this.response25 = value;
            case 26 -> this.response26 = value;
            case 27 -> this.response27 = value;
            case 28 -> this.response28 = value;
            case 29 -> this.response29 = value;
            case 30 -> this.response30 = value;
            default -> throw new SurveyException(SurveyErrorCode.INVALID_RESPONSE_INDEX);
        }
    }

    public void updateTextResponse(String textResponse) {
        this.textResponse = textResponse;
    }

    public void updateResponseStatusToDone() {

        this.responseStatus = ResponseStatus.DONE;
    }

    public void updateResponseStatus() {
        if (allResponsesAreNull()) {
            this.responseStatus = ResponseStatus.NOT_STARTED; // 아무 응답도 없음
        } else if (checkAllResponsesFilled()) {
            this.responseStatus = ResponseStatus.DONE; // 모든 응답이 채워짐
        } else {
            this.responseStatus = ResponseStatus.IN_PROGRESS; // 일부만 채워짐
        }
    }

    public boolean checkAllResponsesFilled() {
        return (response1 != null) && (response2 != null) && (response3 != null) &&
                (response4 != null) && (response5 != null) && (response6 != null) &&
                (response7 != null) && (response8 != null) && (response9 != null) &&
                (response10 != null) && (response11 != null) && (response12 != null) &&
                (response13 != null) && (response14 != null) && (response15 != null) &&
                (response16 != null) && (response17 != null) && (response18 != null) &&
                (response19 != null) && (response20 != null) && (response21 != null) &&
                (response22 != null) && (response23 != null) && (response24 != null) &&
                (response25 != null) && (response26 != null) && (response27 != null) &&
                (response28 != null) && (response29 != null) && (response30 != null) &&
                (textResponse != null);
    }

    private boolean allResponsesAreNull() {
        return response1 == null && response2 == null && response3 == null &&
                response4 == null && response5 == null && response6 == null &&
                response7 == null && response8 == null && response9 == null &&
                response10 == null && response11 == null && response12 == null &&
                response13 == null && response14 == null && response15 == null &&
                response16 == null && response17 == null && response18 == null &&
                response19 == null && response20 == null && response21 == null &&
                response22 == null && response23 == null && response24 == null &&
                response25 == null && response26 == null && response27 == null &&
                response28 == null && response29 == null && response30 == null &&
                textResponse == null;
    }

    @Builder(access = lombok.AccessLevel.PRIVATE)
    private BrandResponse(UserEntity user,
                          Brand brand,
                          ResponseStatus responseStatus,
                          Integer response1, Integer response2, Integer response3, Integer response4, Integer response5,
                          Integer response6, Integer response7, Integer response8, Integer response9, Integer response10,
                          Integer response11, Integer response12, Integer response13, Integer response14, Integer response15,
                          Integer response16, Integer response17, Integer response18, Integer response19, Integer response20,
                          Integer response21, Integer response22, Integer response23, Integer response24, Integer response25,
                          Integer response26, Integer response27, Integer response28, Integer response29, Integer response30, String textResponse) {
        this.user = user;
        this.brand = brand;
        this.responseStatus = responseStatus;
        this.response1 = response1;
        this.response2 = response2;
        this.response3 = response3;
        this.response4 = response4;
        this.response5 = response5;
        this.response6 = response6;
        this.response7 = response7;
        this.response8 = response8;
        this.response9 = response9;
        this.response10 = response10;
        this.response11 = response11;
        this.response12 = response12;
        this.response13 = response13;
        this.response14 = response14;
        this.response15 = response15;
        this.response16 = response16;
        this.response17 = response17;
        this.response18 = response18;
        this.response19 = response19;
        this.response20 = response20;
        this.response21 = response21;
        this.response22 = response22;
        this.response23 = response23;
        this.response24 = response24;
        this.response25 = response25;
        this.response26 = response26;
        this.response27 = response27;
        this.response28 = response28;
        this.response29 = response29;
        this.response30 = response30;
        this.textResponse = textResponse;
    }
}
