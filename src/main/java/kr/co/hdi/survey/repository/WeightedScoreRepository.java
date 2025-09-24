package kr.co.hdi.survey.repository;

import kr.co.hdi.survey.domain.WeightedScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightedScoreRepository extends JpaRepository<WeightedScore, Long> {
}
