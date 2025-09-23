package kr.co.hdi.survey.repository;

import kr.co.hdi.survey.domain.ProductResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductResponseRepository extends JpaRepository<ProductResponse, Long> {

    List<ProductResponse> findAllByUserId(Long userId);
}
