package kr.co.hdi.admin.assignment.service;

import kr.co.hdi.admin.assignment.dto.query.AssignmentDiff;
import kr.co.hdi.admin.assignment.dto.query.AssignmentRow;
import kr.co.hdi.admin.assignment.dto.query.TeamAssignmentBlock;
import kr.co.hdi.admin.assignment.dto.request.AssignmentDataRequest;
import kr.co.hdi.admin.assignment.dto.response.AssignmentDataResponse;
import kr.co.hdi.admin.assignment.dto.response.AssignmentImportResultResponse;
import kr.co.hdi.admin.assignment.dto.response.AssignmentResponse;
import kr.co.hdi.admin.assignment.exception.AssignmentErrorCode;
import kr.co.hdi.admin.assignment.exception.AssignmentException;
import kr.co.hdi.admin.data.dto.request.DataIdsRequest;
import kr.co.hdi.admin.survey.dto.response.SurveyResponse;
import kr.co.hdi.admin.survey.dto.response.SurveyRoundResponse;
import kr.co.hdi.admin.user.dto.response.ExpertNameResponse;
import kr.co.hdi.domain.assignment.entity.IndustryDataAssignment;
import kr.co.hdi.domain.assignment.repository.IndustryDataAssignmentRepository;
import kr.co.hdi.domain.data.entity.IndustryData;
import kr.co.hdi.domain.data.repository.IndustryDataRepository;
import kr.co.hdi.domain.user.entity.Role;
import kr.co.hdi.domain.user.entity.UserEntity;
import kr.co.hdi.domain.user.entity.UserType;
import kr.co.hdi.domain.user.exception.AuthErrorCode;
import kr.co.hdi.domain.user.exception.AuthException;
import kr.co.hdi.domain.user.repository.UserRepository;
import kr.co.hdi.domain.year.entity.AssessmentRound;
import kr.co.hdi.domain.year.entity.UserYearRound;
import kr.co.hdi.domain.year.entity.Year;
import kr.co.hdi.domain.year.enums.DomainType;
import kr.co.hdi.domain.year.repository.AssessmentRoundRepository;
import kr.co.hdi.domain.year.repository.UserYearRoundRepository;
import kr.co.hdi.domain.year.repository.YearRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static kr.co.hdi.admin.assignment.exception.AssignmentErrorCode.USER_NOT_PARTICIPATED_IN_ASSESSMENT_ROUND;

@Service
@RequiredArgsConstructor
public class IndustryAssignmentService implements AssignmentService {

    private final YearRepository yearRepository;
    private final UserRepository userRepository;
    private final IndustryDataRepository industryDataRepository;
    private final UserYearRoundRepository userYearRoundRepository;
    private final AssessmentRoundRepository assessmentRoundRepository;
    private final IndustryDataAssignmentRepository industryDataAssignmentRepository;
    private final AssignmentExcelParser assignmentExcelParser;

    @Override
    public DomainType getDomainType() {
        return DomainType.INDUSTRY;
    }

    /*
    전문가 검색 (이름으로)
     */
    public List<ExpertNameResponse> searchExpertByName(UserType type, String q) {

        return userRepository.findExpertNamesByUserTypeAndName(type, q, Role.USER);
    }

    /*
    매칭 연도-차수 목록 전체 조회
     */
    public List<SurveyResponse> getAssignmentYearRoundList(DomainType type) {

        List<Year> years = yearRepository.findAllByTypeAndDeletedAtIsNullOrderByCreatedAtAsc(type);
        List<AssessmentRound> rounds = assessmentRoundRepository.findAllWithYearByDomainType(type);

        Map<Long, LocalDateTime> roundUpdatedMap = getRoundUpdatedMap(rounds);
        Map<Long, List<SurveyRoundResponse>> roundsByYearId = groupRoundsByYear(rounds, roundUpdatedMap);

        return buildSurveyResponses(years, roundsByYearId);
    }

    private Map<Long, LocalDateTime> getRoundUpdatedMap(List<AssessmentRound> rounds) {
        return rounds.stream()
                .collect(Collectors.toMap(
                        AssessmentRound::getId,
                        r -> Optional.ofNullable(
                                industryDataAssignmentRepository
                                        .findLastModifiedAtByAssessmentRound(r.getId())
                        ).orElse(r.getUpdatedAt())
                ));
    }

    private Map<Long, List<SurveyRoundResponse>> groupRoundsByYear(
            List<AssessmentRound> rounds,
            Map<Long, LocalDateTime> roundUpdatedMap
    ) {
        return rounds.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getYear().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                r -> SurveyRoundResponse.of(
                                        r,
                                        roundUpdatedMap.get(r.getId())
                                ),
                                Collectors.toList()
                        )
                ));
    }

    private List<SurveyResponse> buildSurveyResponses(
            List<Year> years,
            Map<Long, List<SurveyRoundResponse>> roundsByYearId
    ) {
        return years.stream()
                .map(y -> {

                    List<SurveyRoundResponse> roundResponses =
                            roundsByYearId.getOrDefault(y.getId(), List.of());

                    LocalDateTime yearUpdatedAt = roundResponses.stream()
                            .map(SurveyRoundResponse::updatedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(y.getUpdatedAt());

                    return new SurveyResponse(
                            y.getId(),
                            y.getYear(),
                            yearUpdatedAt,
                            y.getCreatedAt(),
                            roundResponses
                    );
                })
                .toList();
    }

    /*
    해당 차수의 데이터셋 매칭 전체 조회
     */
    @Override
    public List<AssignmentResponse> getDatasetAssignment(Long assessmentRoundId, String q) {

        List<AssignmentRow> rows = industryDataAssignmentRepository.findIndustryDataAssignment(assessmentRoundId, q);
        if (rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
                .collect(Collectors.groupingBy(AssignmentRow::userId))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toAssignmentResponse(entry.getValue()))
                .toList();
    }

    /*
    데이터셋 매칭 전문가별 조회
     */
    @Override
    public AssignmentResponse getDatasetAssignmentByUser(Long assessmentRoundId, Long userId) {

        List<AssignmentRow> rows = industryDataAssignmentRepository.findIndustryDataAssignmentByUser(assessmentRoundId, userId);
        if (rows.isEmpty()) {
            return null;
        }

        return toAssignmentResponse(rows);
    }

    private AssignmentResponse toAssignmentResponse(List<AssignmentRow> rows) {
        AssignmentRow first = rows.get(0);

        return new AssignmentResponse(
                first.userId(),
                first.username(),
                rows.stream()
                        .sorted(Comparator.comparing(AssignmentRow::dataCode))
                        .map(this::toAssignmentData)
                        .toList()
        );
    }

    private AssignmentDataResponse toAssignmentData(AssignmentRow row) {
        return new AssignmentDataResponse(
                row.dataId(),
                row.dataCode()
        );
    }

    /*
    데이터셋 매칭 수정
    1. 기존에 할당된 데이터 id 조회
    2. 새로 요청된 데이터 id 조회
    3. 갱신된 정보 파악 (삭제할 id, 추가할 id)
     */
    @Override
    @Transactional
    public void updateDatasetAssignment(
            Long assessmentRoundId,
            Long memberId,
            DataIdsRequest request) {

        UserYearRound userYearRound = getUserYearRound(assessmentRoundId, memberId);
        AssignmentDiff diff = calculateDiff(userYearRound, request);
        AssessmentRound assessmentRound = userYearRound.getAssessmentRound();

        deleteRemovedAssignments(userYearRound, diff);
        addNewAssignments(userYearRound, diff, assessmentRound.getYear());
    }

    private UserYearRound getUserYearRound(Long assessmentRoundId, Long memberId) {

        return userYearRoundRepository.findByAssessmentRoundIdAndUserId(assessmentRoundId, memberId)
                .orElseThrow(() -> new AssignmentException(USER_NOT_PARTICIPATED_IN_ASSESSMENT_ROUND));
    }

    private AssignmentDiff calculateDiff(UserYearRound userYearRound, DataIdsRequest request) {

        // 기존에 할당된 데이터 ids
        Set<Long> existingIds = industryDataAssignmentRepository.findByUserYearRound(userYearRound)
                .stream()
                .map(a -> a.getIndustryData().getId())
                .collect(Collectors.toSet());

        // 새로 수정된 데이터 ids
        Set<Long> requestedIds = new HashSet<>(request.ids());

        return AssignmentDiff.of(existingIds, requestedIds);
    }

    private void deleteRemovedAssignments(UserYearRound userYearRound, AssignmentDiff diff) {

        if (diff.toRemove().isEmpty()) {
            return;
        }
        industryDataAssignmentRepository.deleteByUserYearRoundAndIndustryDataIds(userYearRound, diff.toRemove());
    }

    private void addNewAssignments(UserYearRound userYearRound, AssignmentDiff diff, Year year) {

        if (diff.toAdd().isEmpty()) {
            return;
        }

        List<IndustryData> industryDataList = industryDataRepository.findAllById(diff.toAdd());
        industryDataAssignmentRepository.saveAll(
                IndustryDataAssignment.createAll(userYearRound, industryDataList, year.getSurveyCount())
        );
    }

    /*
    전문가와 데이터셋 매칭 등록
    1. 해당 연도의 차수에 전문가 등록 (UserYearRound 등록)
    2. 전문가에게 데이터셋 할당 (userYearRound와 dataIds를 Assignment에 저장)
     */
    @Override
    @Transactional
    public void createDatasetAssignment(
            Long assessmentRoundId,
            AssignmentDataRequest request) {

        UserEntity user = getUser(request.memberId());
        AssessmentRound assessmentRound = getAssessmentRound(assessmentRoundId);
        List<IndustryData> industryDataList = getIndustryData(request.datasetsIds());

        UserYearRound userYearRound =
                getOrCreateUserYearRound(user, assessmentRound);

        createIndustryDataAssignments(userYearRound, industryDataList, assessmentRound.getYear());
    }

    private UserYearRound getOrCreateUserYearRound(
            UserEntity user,
            AssessmentRound assessmentRound
    ) {
        return userYearRoundRepository
                .findByUserAndAssessmentRound(user, assessmentRound)
                .orElseGet(() -> userYearRoundRepository.save(
                        new UserYearRound(user, assessmentRound)
                ));
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
    }

    private AssessmentRound getAssessmentRound(Long id) {
        return assessmentRoundRepository.findById(id)
                .orElseThrow(() -> new AssignmentException(AssignmentErrorCode.ASSESSMENT_ROUND_NOT_FOUND));
    }

    private List<IndustryData> getIndustryData(List<Long> ids) {
        return industryDataRepository.findAllById(ids);
    }

    private UserYearRound createUserYearRound(
            UserEntity user,
            AssessmentRound assessmentRound) {

        UserYearRound userYearRound = UserYearRound.builder()
                .user(user)
                .assessmentRound(assessmentRound)
                .build();

        return userYearRoundRepository.save(userYearRound);
    }

    private void createIndustryDataAssignments(
            UserYearRound userYearRound,
            List<IndustryData> industryDataList,
            Year year) {

        industryDataAssignmentRepository.saveAll(
                IndustryDataAssignment.createAll(userYearRound, industryDataList, year.getSurveyCount())
        );
    }

    /*
    전문가-데이터 매칭 엑셀 업로드 (VisualAssignmentService.importDatasetAssignmentExcel 참고)
     */
    @Override
    @Transactional
    public AssignmentImportResultResponse importDatasetAssignmentExcel(Long assessmentRoundId, MultipartFile file) {

        AssessmentRound assessmentRound = getAssessmentRound(assessmentRoundId);
        Year year = assessmentRound.getYear();

        List<TeamAssignmentBlock> blocks = assignmentExcelParser.parse(file);

        Map<String, IndustryData> dataByCode = industryDataRepository.findByYearIdAndDeletedAtIsNull(year.getId())
                .stream()
                .collect(Collectors.toMap(IndustryData::getOriginalId, d -> d, (a, b) -> a));

        List<String> warnings = new ArrayList<>();
        int teamsProcessed = 0;
        int added = 0;
        int removed = 0;

        for (TeamAssignmentBlock block : blocks) {

            String teamLabel = block.team() == null ? "(팀명 없음)" : block.team();

            if (block.connectId() == null) {
                warnings.add("[%s] connect_id가 비어있어 건너뜀".formatted(teamLabel));
                continue;
            }

            Optional<UserEntity> userOpt = userRepository.findByEmail(block.connectId());
            if (userOpt.isEmpty()) {
                warnings.add("[%s] 존재하지 않는 계정(connect_id=%s) - 전문가 계정을 먼저 등록해주세요."
                        .formatted(teamLabel, block.connectId()));
                continue;
            }
            UserEntity user = userOpt.get();

            if (block.password() != null && !block.password().equals(user.getPassword())) {
                warnings.add("[%s] 비밀번호가 등록된 계정 정보와 다릅니다(connect_id=%s) - 매칭은 그대로 진행했습니다."
                        .formatted(teamLabel, block.connectId()));
            }

            UserYearRound userYearRound = getOrCreateUserYearRound(user, assessmentRound);
            userYearRound.updateTeam(block.team());

            List<Long> resolvedIds = new ArrayList<>();
            for (String code : block.dataCodes()) {
                IndustryData data = dataByCode.get(code);
                if (data == null) {
                    warnings.add("[%s] 존재하지 않는 데이터 아이디: %s".formatted(teamLabel, code));
                    continue;
                }
                resolvedIds.add(data.getId());
            }

            AssignmentDiff diff = calculateDiff(userYearRound, new DataIdsRequest(resolvedIds));
            deleteRemovedAssignments(userYearRound, diff);
            addNewAssignments(userYearRound, diff, year);

            added += diff.toAdd().size();
            removed += diff.toRemove().size();
            teamsProcessed++;
        }

        return new AssignmentImportResultResponse(teamsProcessed, added, removed, warnings);
    }

    /*
    전문가와 산업 디자인 데이터셋 할당 엑셀 다운로드
     */
    public byte[] exportDataAssignments(Long assessmentRoundId) {

        List<AssignmentRow> rows = industryDataAssignmentRepository.findIndustryDataAssignment(assessmentRoundId, "");

        Map<Long, List<AssignmentRow>> groupedByUser =
                rows.stream()
                        .collect(Collectors.groupingBy(AssignmentRow::userId));

        List<Long> dataIds = rows.stream()
                .map(AssignmentRow::dataId)
                .distinct()
                .toList();

        Map<Long, IndustryData> industryDataMap =
                industryDataRepository.findAllById(dataIds)
                        .stream()
                        .collect(Collectors.toMap(IndustryData::getId, d -> d));

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (Map.Entry<Long, List<AssignmentRow>> entry : groupedByUser.entrySet()) {

                List<AssignmentRow> userRows = entry.getValue();
                String sheetName = userRows.get(0).username();

                Sheet sheet = wb.createSheet(sheetName);

                String[] headers = {
                        "Code",
                        "CompanyName",
                        "ProductName",
                        "ModelName",
                        "Price",
                        "Material",
                        "Size",
                        "Weight",
                        "ReferenceUrl",
                        "RegisteredAt",
                        "ProductPath",
                        "ProductTypeName",
                        "DataCategory",

                        // 2026
                        "NoiseCancelling",
                        "Codec",
                        "ExtraFeatures",
                        "ControlType",
                        "Waterproof",
                        "MaxPlayTime",
                        "ChargeTime",
                        "Usage",
                        "ShoppingUrl",
                        "SoundOutput",
                        "Connectivity"
                };

                Row headerRow = sheet.createRow(0);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headers[c]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                for (AssignmentRow r : userRows) {

                    IndustryData data = industryDataMap.get(r.dataId());
                    if (data == null) {
                        continue;
                    }

                    Row row = sheet.createRow(rowIdx++);
                    int c = 0;

                    row.createCell(c++).setCellValue(nvl(data.getOriginalId()));
                    row.createCell(c++).setCellValue(nvl(data.getCompanyName()));
                    row.createCell(c++).setCellValue(nvl(data.getProductName()));
                    row.createCell(c++).setCellValue(nvl(data.getModelName()));
                    row.createCell(c++).setCellValue(nvl(data.getPrice()));
                    row.createCell(c++).setCellValue(nvl(data.getMaterial()));
                    row.createCell(c++).setCellValue(nvl(data.getSize()));
                    row.createCell(c++).setCellValue(nvl(data.getWeight()));
                    row.createCell(c++).setCellValue(nvl(data.getReferenceUrl()));
                    row.createCell(c++).setCellValue(nvl(data.getRegisteredAt()));
                    row.createCell(c++).setCellValue(nvl(data.getProductPath()));
                    row.createCell(c++).setCellValue(nvl(data.getProductTypeName()));
                    row.createCell(c++).setCellValue(nvl(data.getIndustryDataCategory()));


                    // 2026
                    row.createCell(c++).setCellValue(nvl(data.getNoiseCancelling()));
                    row.createCell(c++).setCellValue(nvl(data.getCodec()));
                    row.createCell(c++).setCellValue(nvl(data.getExtraFeatures()));
                    row.createCell(c++).setCellValue(nvl(data.getControlType()));
                    row.createCell(c++).setCellValue(nvl(data.getWaterproof()));
                    row.createCell(c++).setCellValue(nvl(data.getMaxPlayTime()));
                    row.createCell(c++).setCellValue(nvl(data.getChargeTime()));
                    row.createCell(c++).setCellValue(nvl(data.getUsage()));
                    row.createCell(c++).setCellValue(nvl(data.getShoppingUrl()));
                    row.createCell(c++).setCellValue(nvl(data.getSoundOutput()));
                    row.createCell(c++).setCellValue(nvl(data.getConnectivity()));

                }

                for (int c = 0; c < headers.length; c++) {
                    sheet.autoSizeColumn(c);
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("엑셀 생성 실패", e);
        }
    }

    private String nvl(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
