package kr.co.hdi.domain.data.repository;

import kr.co.hdi.admin.data.dto.response.IndustryDataIdsResponse;
import kr.co.hdi.admin.data.dto.response.VisualDataIdsResponse;
import kr.co.hdi.domain.data.entity.IndustryData;
import kr.co.hdi.domain.data.entity.VisualData;
import kr.co.hdi.domain.data.enums.IndustryDataCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndustryDataRepository extends JpaRepository<IndustryData, Long>, IndustryDataRepositoryCustom {

    @Query("""
    SELECT i
    FROM IndustryData i
    WHERE i.deletedAt IS NULL
        AND i.year.id = :yearId
    """)
    List<IndustryData> findByYearIdAndDeletedAtIsNull(@Param("yearId") Long yearId);

    @Query("""
    SELECT new kr.co.hdi.admin.data.dto.response.IndustryDataIdsResponse(
        i.id,
        i.originalId)
    FROM IndustryData i
    WHERE i.deletedAt IS NULL
        AND i.year.id = :yearId
    ORDER BY i.originalIdInteger ASC
    """)
    List<IndustryDataIdsResponse> findIdByYearId(@Param("yearId") Long yearId);

    Optional<IndustryData> findByIdAndDeletedAtIsNull(Long id);

    List<IndustryData> findByIdInAndDeletedAtIsNull(List<Long> ids);

    @Query("""
        SELECT MAX(i.updatedAt)
        FROM IndustryData i
        WHERE i.year.id = :yearId
          AND i.deletedAt IS NULL
    """)
    Optional<LocalDateTime> findLastModifiedAtByYearId(Long yearId);

    Optional<IndustryData> findByOriginalId(String dataCode);

    @Query("""
    SELECT i
    FROM IndustryData i
    WHERE i.deletedAt IS NULL
        AND i.year.id = :yearId
        AND i.industryDataCategory = :category
        AND i.originalId = :code
    """)
    List<IndustryData> findAllByYearIdAndCategoryAndOriginalId(
            @Param("yearId") Long yearId,
            @Param("category") IndustryDataCategory category,
            @Param("code") String code
    );

}


