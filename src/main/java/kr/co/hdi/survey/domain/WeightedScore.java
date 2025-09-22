package kr.co.hdi.survey.domain;

import jakarta.persistence.*;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import kr.co.hdi.user.domain.UserEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class WeightedScore extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private DatasetCategory category;

    private int score1;   // 심미성

    private int score2;   // 조형성

    private int score3;   // 독창성

    private int score4;   // 사용성

    private int score5;   // 기능성

    private int score6;   // 윤리성

    private int score7;   // 경제성

    private int score8;   // 목적성

}
