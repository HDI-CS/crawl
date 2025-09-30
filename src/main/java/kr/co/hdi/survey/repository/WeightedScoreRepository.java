package kr.co.hdi.survey.repository;

import kr.co.hdi.survey.domain.WeightedScore;
import kr.co.hdi.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeightedScoreRepository extends JpaRepository<WeightedScore, Long> {

    List<WeightedScore> findByUser(UserEntity user);
}
