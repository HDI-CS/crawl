package kr.co.hdi.dataset.repository;

import kr.co.hdi.dataset.domain.BrandDatasetAssignment;
import kr.co.hdi.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandDatasetAssignmentRepository extends JpaRepository<BrandDatasetAssignment, Long> {

    List<BrandDatasetAssignment> findAllByUserId(Long userId);

    long countByUser(UserEntity user);
}
