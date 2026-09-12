package kr.co.hdi.domain.year.entity;

import jakarta.persistence.*;
import kr.co.hdi.domain.user.entity.UserEntity;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserYearRound extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "user_year_round_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    private AssessmentRound assessmentRound;

    // 엑셀 매칭 업로드 시 부여되는 팀 라벨 (예: "팀A"). 회차별로 달라질 수 있어 유저가 아닌 여기에 둔다.
    private String team;

    @Builder
    public UserYearRound(UserEntity user, AssessmentRound assessmentRound, String team) {
        this.user = user;
        this.assessmentRound = assessmentRound;
        this.team = team;
    }

    public UserYearRound(UserEntity user, AssessmentRound assessmentRound) {
        this(user, assessmentRound, null);
    }

    public void updateTeam(String team) {
        this.team = team;
    }
}
