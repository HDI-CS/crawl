package kr.co.hdi.domain.response.entity;

import jakarta.persistence.*;
import kr.co.hdi.domain.data.entity.IndustryData;
import kr.co.hdi.domain.survey.entity.IndustrySurvey;
import kr.co.hdi.domain.year.entity.UserYearRound;
import kr.co.hdi.global.domain.BaseTimeEntityWithDeletion;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_industryResponse_userYearRound_survey_data",
                columnNames = {
                        "user_year_round_user_year_round_id",
                        "industry_survey_industry_survey_id",
                        "industry_data_industry_data_id"
                }
        )
)
public class IndustryResponse extends BaseTimeEntityWithDeletion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "industry_response_id")
    private Long id;

    private Integer numberResponse;  // 정량

    @Column(columnDefinition = "text")
    private String textResponse;  // 정성

    @ManyToOne(fetch = FetchType.LAZY)
    private IndustryData industryData;

    @ManyToOne(fetch = FetchType.LAZY)
    private IndustrySurvey industrySurvey;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserYearRound userYearRound;

    public void updateNumberResponse(Integer numberResponse) {
        this.numberResponse = numberResponse;
    }

    public void updateTextResponse(String textResponse) {
        this.textResponse = textResponse;
    }

    @Builder
    public IndustryResponse(Integer numberResponse, String textResponse, IndustryData industryData, IndustrySurvey industrySurvey, UserYearRound userYearRound) {
        this.numberResponse = numberResponse;
        this.textResponse = textResponse;
        this.industryData = industryData;
        this.industrySurvey = industrySurvey;
        this.userYearRound = userYearRound;
    }
}
