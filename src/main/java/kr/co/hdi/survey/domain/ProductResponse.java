package kr.co.hdi.survey.domain;

import jakarta.persistence.*;
import kr.co.hdi.crawl.domain.Product;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import kr.co.hdi.survey.exception.SurveyErrorCode;
import kr.co.hdi.survey.exception.SurveyException;
import kr.co.hdi.user.domain.UserEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ProductResponse extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

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
    private Integer response31;
    private Integer response32;
    private Integer response33;
    private Integer response34;
    private Integer response35;
    private Integer response36;
    private Integer response37;
    private Integer response38;
    private Integer response39;
    private Integer response40;
    private Integer response41;


    private String textResponse;


    public static ProductResponse createProductResponse(UserEntity user, Product product) {

        return ProductResponse.builder()
                .user(user)
                .product(product)
                .responseStatus(ResponseStatus.NOT_STARTED)
                .build();
    }

    @lombok.Builder(access = lombok.AccessLevel.PRIVATE)
    private ProductResponse(UserEntity user,
                            Product product,
                            ResponseStatus responseStatus,
                            int response1, int response2, int response3, int response4, int response5,
                            int response6, int response7, int response8, int response9, int response10,
                            int response11, int response12, int response13, int response14, int response15,
                            int response16, int response17, int response18, int response19, int response20,
                            int response21, int response22, int response23, int response24, int response25,
                            int response26, int response27, int response28, int response29, int response30,
                            int response31, int response32, int response33, int response34, int response35,
                            int response36, int response37, int response38, int response39, int response40,
                            int response41) {
        this.user = user;
        this.product = product;
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
        this.response31 = response31;
        this.response32 = response32;
        this.response33 = response33;
        this.response34 = response34;
        this.response35 = response35;
        this.response36 = response36;
        this.response37 = response37;
        this.response38 = response38;
        this.response39 = response39;
        this.response40 = response40;
        this.response41 = response41;
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
            case 31 -> this.response31 = value;
            case 32 -> this.response32 = value;
            case 33 -> this.response33 = value;
            case 34 -> this.response34 = value;
            case 35 -> this.response35 = value;
            case 36 -> this.response36 = value;
            case 37 -> this.response37 = value;
            case 38 -> this.response38 = value;
            case 39 -> this.response39 = value;
            case 40 -> this.response40 = value;
            case 41 -> this.response41 = value;
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

    public boolean checkAllResponsesFilled () {
        return (response1 != 0) && (response2 != 0) && (response3 != 0) &&
                (response4 != 0) && (response5 != 0) && (response6 != 0) &&
                (response7 != 0) && (response8 != 0) && (response9 != 0) &&
                (response10 != 0) && (response11 != 0) && (response12 != 0) &&
                (response13 != 0) && (response14 != 0) && (response15 != 0) &&
                (response16 != 0) && (response17 != 0) && (response18 != 0) &&
                (response19 != 0) && (response20 != 0) && (response21 != 0) &&
                (response22 != 0) && (response23 != 0) && (response24 != 0) &&
                (response25 != 0) && (response26 != 0) && (response27 != 0) &&
                (response28 != 0) && (response29 != 0) && (response30 != 0) &&
                (response31 != 0) && (response32 != 0) && (response33 != 0) &&
                (response34 != 0) && (response35 != 0) && (response36 != 0) &&
                (response37 != 0) && (response38 != 0) && (response39 != 0) &&
                (response40 != 0) && (response41 != 0) &&
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
                response31 == null && response32 == null && response33 == null &&
                response34 == null && response35 == null && response36 == null &&
                response37 == null && response38 == null && response39 == null &&
                response40 == null && response41 == null &&
                textResponse == null;
    }

}
