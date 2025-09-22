package kr.co.hdi.survey.domain;

import jakarta.persistence.*;
import kr.co.hdi.dataset.domain.Brand;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import kr.co.hdi.user.domain.UserEntity;
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
}
