package kr.co.hdi.survey.domain;

import jakarta.persistence.*;
import kr.co.hdi.crawl.domain.Product;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
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

    private int response1;

    private int response2;

    private int response3;

    private int response4;

    private int response5;

    private int response6;

    private int response7;

    private int response8;

    private int response9;

    private int response10;

    private int response11;

    private int response12;

    private int response13;

    private int response14;

    private int response15;

    private int response16;

    private int response17;

    private int response18;

    private int response19;

    private int response20;

    private int response21;

    private int response22;

    private int response23;

    private int response24;

    private int response25;

    private int response26;

    private int response27;

    private int response28;

    private int response29;

    private int response30;

    private int response31;

    private int response32;

    private int response33;

    private int response34;

    private int response35;

    private int response36;

    private int response37;

    private int response38;

    private int response39;

    private int response40;

    private int response41;

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
}
