package kr.co.hdi.survey.repository;

import kr.co.hdi.survey.domain.ProductSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSurveyRepository extends JpaRepository<ProductSurvey, Long> {
}
