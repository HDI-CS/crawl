package kr.co.hdi.domain.data.repository;

import kr.co.hdi.admin.data.dto.response.VisualDataIdsResponse;
import kr.co.hdi.domain.data.entity.VisualData;
import kr.co.hdi.domain.data.enums.VisualDataCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisualDataRepository extends JpaRepository<VisualData, Long>, VisualDataRepositoryCustom {

    @Query("""
    SELECT v
    FROM VisualData v
    WHERE v.deletedAt IS NULL
        AND v.year.id = :yearId
    ORDER BY v.brandCodeInteger ASC
    """)
    List<VisualData> findByYearIdAndDeletedAtIsNull(@Param("yearId") Long yearId);

    @Query("""
    SELECT new kr.co.hdi.admin.data.dto.response.VisualDataIdsResponse(
        v.id,
        v.brandCode)
    FROM VisualData v
    WHERE v.deletedAt IS NULL
        AND v.year.id = :yearId
    ORDER BY v.brandCodeInteger ASC
    """)
    List<VisualDataIdsResponse> findIdByYearId(@Param("yearId") Long yearId);

    Optional<VisualData> findByIdAndDeletedAtIsNull(Long id);

    List<VisualData> findByIdInAndDeletedAtIsNull(List<Long> ids);

    @Query("""
        SELECT MAX(v.updatedAt)
        FROM VisualData v
        WHERE v.year.id = :yearId
          AND v.deletedAt IS NULL
    """)
    Optional<LocalDateTime> findLastModifiedAtByYearId(Long yearId);


    // 삭제
    List<VisualData> findByBrandCode(String dataCode);
    List<VisualData> findByBrandCodeAndVisualDataCategory(
            String brandCode,
            VisualDataCategory category
    );
}
