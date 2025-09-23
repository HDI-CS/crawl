package kr.co.hdi.survey.repository;

import kr.co.hdi.survey.domain.BrandResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandResponseRepository extends JpaRepository<BrandResponse, Long> {

    List<BrandResponse> findAllByUserId(Long userId);
}
