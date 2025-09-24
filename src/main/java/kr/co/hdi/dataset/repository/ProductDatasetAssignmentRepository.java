package kr.co.hdi.dataset.repository;

import kr.co.hdi.dataset.domain.ProductDatasetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDatasetAssignmentRepository extends JpaRepository<ProductDatasetAssignment, Long> {

    List<ProductDatasetAssignment> findAllByUserId(Long userId);
}
