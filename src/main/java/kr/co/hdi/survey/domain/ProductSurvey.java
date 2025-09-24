package kr.co.hdi.survey.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ProductSurvey extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String survey1;

    private String survey2;

    private String survey3;

    private String survey4;

    private String survey5;

    private String survey6;

    private String survey7;

    private String survey8;

    private String survey9;

    private String survey10;

    private String survey11;

    private String survey12;

    private String survey13;

    private String survey14;

    private String survey15;

    private String survey16;

    private String survey17;

    private String survey18;

    private String survey19;

    private String survey20;

    private String survey21;

    private String survey22;

    private String survey23;

    private String survey24;

    private String survey25;

    private String survey26;

    private String survey27;

    private String survey28;

    private String survey29;

    private String survey30;

    private String survey31;

    private String survey32;

    private String survey33;

    private String survey34;

    private String survey35;

    private String survey36;

    private String survey37;

    private String survey38;

    private String survey39;

    private String survey40;

    private String survey41;

    private String textSurvey;
}
