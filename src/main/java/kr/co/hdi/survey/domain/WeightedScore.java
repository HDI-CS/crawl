package kr.co.hdi.survey.domain;

import jakarta.persistence.*;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import kr.co.hdi.user.domain.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;
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

    public static WeightedScore createWeightedScore(
            UserEntity user, DatasetCategory category,
            int score1, int score2, int score3, int score4,
            int score5, int score6, int score7, int score8) {

        return WeightedScore.builder()
                .user(user)
                .category(category)
                .score1(score1)
                .score2(score2)
                .score3(score3)
                .score4(score4)
                .score5(score5)
                .score6(score6)
                .score7(score7)
                .score8(score8)
                .build();
    }

    @Builder(access = PRIVATE)
    private WeightedScore(UserEntity user, DatasetCategory category,
                         int score1, int score2, int score3, int score4,
                         int score5, int score6, int score7, int score8) {
        this.user = user;
        this.category = category;
        this.score1 = score1;
        this.score2 = score2;
        this.score3 = score3;
        this.score4 = score4;
        this.score5 = score5;
        this.score6 = score6;
        this.score7 = score7;
        this.score8 = score8;
    }
}
