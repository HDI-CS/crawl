package kr.co.hdi.admin.evaluation.service;

import kr.co.hdi.admin.evaluation.dto.enums.EvaluationType;
import kr.co.hdi.admin.evaluation.dto.response.EvaluationAnswerByDataResponse;
import kr.co.hdi.admin.evaluation.dto.response.EvaluationAnswerByMemberResponse;
import kr.co.hdi.admin.evaluation.dto.response.EvaluationStatusByMemberResponse;
import kr.co.hdi.admin.evaluation.dto.response.EvaluationStatusResponse;
import kr.co.hdi.admin.evaluation.exeption.EvaluationErrorCode;
import kr.co.hdi.admin.evaluation.exeption.EvaluationException;
import kr.co.hdi.domain.assignment.query.DataIdCodePair;
import kr.co.hdi.domain.assignment.query.UserDataIdCodePair;
import kr.co.hdi.domain.assignment.query.UserDataPair;
import kr.co.hdi.domain.assignment.repository.VisualDataAssignmentRepository;
import kr.co.hdi.domain.currentSurvey.entity.CurrentVisualCategory;
import kr.co.hdi.domain.currentSurvey.repository.CurrentVisualCategoryRepository;
import kr.co.hdi.domain.data.enums.VisualDataCategory;
import kr.co.hdi.domain.response.entity.VisualResponse;
import kr.co.hdi.domain.response.entity.VisualWeightedScore;
import kr.co.hdi.domain.response.query.UserResponsePair;
import kr.co.hdi.domain.response.query.UserSurveyResponsePair;
import kr.co.hdi.domain.response.query.UserVisualWeightedScorePair;
import kr.co.hdi.domain.response.repository.VisualResponseRepository;
import kr.co.hdi.domain.response.repository.VisualWeightedScoreRepository;
import kr.co.hdi.domain.survey.entity.VisualSurvey;
import kr.co.hdi.domain.survey.repository.VisualSurveyRepository;
import kr.co.hdi.domain.user.entity.UserEntity;
import kr.co.hdi.domain.user.entity.UserType;
import kr.co.hdi.domain.user.repository.UserRepository;
import kr.co.hdi.domain.year.entity.AssessmentRound;
import kr.co.hdi.domain.year.enums.DomainType;
import kr.co.hdi.domain.year.repository.AssessmentRoundRepository;
import kr.co.hdi.domain.year.repository.UserYearRoundRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisualEvaluationService implements EvaluationService {

    private final UserRepository userRepository;
    private final VisualResponseRepository visualResponseRepository;
    private final VisualWeightedScoreRepository visualWeightedScoreRepository;
    private final VisualDataAssignmentRepository visualDataAssignmentRepository;
    private final AssessmentRoundRepository assessmentRoundRepository;
    private final VisualSurveyRepository visualSurveyRepository;
    private final UserYearRoundRepository userYearRoundRepository;
    private final CurrentVisualCategoryRepository currentVisualCategoryRepository;

    @Override
    public DomainType getDomainType() {
        return DomainType.VISUAL;
    }

    /*
    평가 응답 전체 조회
    0. userType 및 surveyCount 할당
    1. 데이터 조회 (전문가, 정성평가 응답, 산업디자인 가중치평가 응답, 할당된 평가데이터)
    2. 전문가별 응답문항/데이터 그룹핑
    3. 응답 생성 (createEvaluationStatus 사용)
     */
    @Override
    public List<EvaluationStatusByMemberResponse> getEvaluationStatus(
            DomainType type,
            Long assessmentRoundId,
            String q
    ) {

        // 평가 회차 조회 및 검증 (Year Fetch Join으로 <surveyCount>에서 추가 쿼리 방지)
        AssessmentRound assessmentRound = assessmentRoundRepository
                .findByIdWithYear(assessmentRoundId)
                .orElseThrow(() -> new EvaluationException(
                        EvaluationErrorCode.ASSESSMENT_ROUND_NOT_FOUND));

        UserType userType = type.toUserType();
        Integer surveyCount = assessmentRound.getYear().getSurveyCount();

        List<UserEntity> users =
                (q == null || q.isBlank())
                        ? userYearRoundRepository.findUsers(userType, assessmentRound)
                        : userYearRoundRepository.findUsersBySearch(userType, assessmentRound, q);
        List<CurrentVisualCategory> categories = currentVisualCategoryRepository.findAll();
        List<UserDataPair> dataAssignments = visualDataAssignmentRepository.findUserDataPairsByAssessmentRoundId(assessmentRoundId);
        List<UserVisualWeightedScorePair> weightedScores = visualWeightedScoreRepository.findPairsByUserYearRound(assessmentRoundId);
        List<UserResponsePair> qualitativeResponses = visualResponseRepository.findPairsByUserYearRound(assessmentRoundId);

        // 전문가-할당데이터 그룹핑 (모든 데이터)
        Map<Long, List<Long>> dataIdsByUserId =
                dataAssignments.stream().collect(groupingBy(
                        UserDataPair::userId,
                        mapping(UserDataPair::dataId, toList())
                ));

        // 전문가-할당데이터-정량평가응답 그룹핑 (응답한 데이터)
        Map<Long, Map<Long, List<UserResponsePair>>> responsesByUser =
                qualitativeResponses.stream()
                        .collect(groupingBy(
                                UserResponsePair::userId,
                                Collectors.groupingBy(UserResponsePair::dataId)
                        ));

        // 전문가-가중치평가응답
        Map<Long, List<UserVisualWeightedScorePair>> weightedByUserId =
                weightedScores.stream()
                        .collect(Collectors.groupingBy(
                                UserVisualWeightedScorePair::userId
                        ));

        return users.stream()
                .map(user ->
                        createEvaluationStatus(
                        user,
                        responsesByUser.getOrDefault(user.getId(), Map.of()),
                        weightedByUserId.get(user.getId()),
                        dataIdsByUserId.getOrDefault(user.getId(), List.of()),
                        surveyCount,
                        categories
                ))
                .toList();
    }

    /*
    평가 응답 전체 조회 응답 생성 헬퍼
    1. 데이터 별 정량평가 상태 확인
    2. 전문가 가중치평가 상태 확인
    3. 응답 생성
     */
    private EvaluationStatusByMemberResponse createEvaluationStatus(
            UserEntity user,
            Map<Long, List<UserResponsePair>> userResponses,
            List<UserVisualWeightedScorePair> weightedScore,
            List<Long> userDataIds,
            Integer surveyCount,
            List<CurrentVisualCategory> categories

    ) {

        // 데이터 id 오름차순으로 완료/미완료 상태를 담는 list
        List<EvaluationStatusResponse> statuses = userDataIds.stream()
                .sorted()
                .map(dataId -> {
                    List<UserResponsePair> list = userResponses.get(dataId);
                    boolean isDone = isQualitativeDone(list, surveyCount);

                    return EvaluationStatusResponse.of(EvaluationType.QUALITATIVE, isDone);

                })
                .collect(Collectors.toCollection(ArrayList::new));

        statuses.add(EvaluationStatusResponse.of(EvaluationType.WEIGHTED, isWeightedDone(weightedScore, categories)));

        return EvaluationStatusByMemberResponse.of(user, statuses);
    }

    /*
    정성 평가 상태 확인 헬퍼
     */
    private boolean isQualitativeDone(List<UserResponsePair> list, Integer surveyCount) {
        if (list == null || list.size() != surveyCount) {
            return false;
        }

        return list.stream().allMatch(r ->
                r.numberResponse() != null ||
                        (r.textResponse() != null && !r.textResponse().isBlank())
        );
    }

    /*
    가중치 평가 상태 확인 헬퍼
     */
    private boolean isWeightedDone(
            List<UserVisualWeightedScorePair> ws,
            List<CurrentVisualCategory> categories) {
        if (ws == null || ws.isEmpty()) return false;

        boolean allCategoriesCovered = categories.stream()
                .allMatch(category -> hasCategoryInScores(ws, category.getCategory()));

        if (!allCategoriesCovered) {
            return false;
        }

        return ws.stream().allMatch(this::isTotalScoreValid);
    }

    private boolean hasCategoryInScores(List<UserVisualWeightedScorePair> scores, VisualDataCategory category) {
        return scores.stream()
                .anyMatch(score ->
                        score.visualDataCategory() != null &&
                                score.visualDataCategory().equals(category)
                );
    }

    private boolean isTotalScoreValid(UserVisualWeightedScorePair vws) {
        int total = nz(vws.score1()) + nz(vws.score2()) +
                nz(vws.score3()) + nz(vws.score4()) +
                nz(vws.score5()) + nz(vws.score6()) +
                nz(vws.score7()) + nz(vws.score8());
        return total == 100;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /*
    특정 전문가 응답 전체 조회
     */
    @Override
    public EvaluationAnswerByMemberResponse getEvaluationByMember(
            DomainType type,
            Long assessmentRoundId,
            Long memberId
    ) {

        // 평가 회차 조회 및 검증 (Year Fetch Join으로 <surveyCount>에서 추가 쿼리 방지)
        AssessmentRound assessmentRound = assessmentRoundRepository
                .findByIdWithYear(assessmentRoundId)
                .orElseThrow(() -> new EvaluationException(
                        EvaluationErrorCode.ASSESSMENT_ROUND_NOT_FOUND));

        UserType userType = type.toUserType();
        UserEntity user = userRepository.findByIdAndUserTypeAndDeletedAtIsNull(memberId, userType)
                .orElseThrow(() -> new EvaluationException(EvaluationErrorCode.USER_NOT_FOUND));

        List<DataIdCodePair> pairs = visualDataAssignmentRepository.findDataIdCodePairsByAssessmentRoundIdAndUserId(assessmentRoundId, memberId);
        List<VisualSurvey> surveys = visualSurveyRepository.findAllByYear(assessmentRound.getYear().getId());
        List<VisualResponse> responses = visualResponseRepository.findAllByAssessmentRoundIdAndMemberId(assessmentRoundId, memberId);

        Map<Long, List<VisualResponse>> responsesByDataId = responses.stream()
                .filter(r -> r.getVisualData() != null)
                .collect(Collectors.groupingBy(r -> r.getVisualData().getId()));

        List<EvaluationAnswerByDataResponse> surveyDatas = pairs.stream()
                .map(pair -> EvaluationAnswerByDataResponse.ofVisual(
                        pair,
                        surveys,
                        responsesByDataId.getOrDefault(pair.dataId(), List.of())
                ))
                .toList();

        return EvaluationAnswerByMemberResponse.of(user, surveyDatas);
    }

    /*
    평가 응답 데이터셋 액셀 다운로드
     */
    @Override
    public byte[] exportEvaluationExcelsZip(
            DomainType type,
            Long assessmentRoundId
    ) {
        AssessmentRound assessmentRound = assessmentRoundRepository
                .findByIdWithYear(assessmentRoundId)
                .orElseThrow(() -> new EvaluationException(EvaluationErrorCode.ASSESSMENT_ROUND_NOT_FOUND));

        UserType userType = type.toUserType();
        Integer surveyCount = Optional.ofNullable(assessmentRound.getYear().getSurveyCount()).orElse(0);

        List<UserEntity> users = userYearRoundRepository.findUsers(userType, assessmentRound);
        List<UserDataIdCodePair> pairs = visualDataAssignmentRepository.findDataIdCodePairsByAssessmentRoundId(assessmentRoundId);
        List<VisualSurvey> surveys = visualSurveyRepository.findAllByYear(assessmentRound.getYear().getId());

        Map<Long, List<UserDataIdCodePair>> pairsByUserId = pairs.stream()
                .collect(Collectors.groupingBy(UserDataIdCodePair::userId));

        Map<Integer, String> surveyContentByNo = surveys.stream()
                .collect(Collectors.toMap(
                        VisualSurvey::getSurveyNumber,
                        VisualSurvey::getSurveyContent
                ));

        List<UserSurveyResponsePair> responses = visualResponseRepository.findAllByUserYearRound(assessmentRoundId);

        Map<Long, Map<Long, Map<Integer, UserSurveyResponsePair>>> responseIndex =
                responses.stream()
                        .collect(Collectors.groupingBy(
                                UserSurveyResponsePair::userId,
                                Collectors.groupingBy(
                                        UserSurveyResponsePair::dataId,
                                        Collectors.toMap(
                                                UserSurveyResponsePair::surveyNumber,
                                                Function.identity()
                                        )
                                )
                        ));

        List<VisualWeightedScore> weightedScores =
                visualWeightedScoreRepository.findAllByUserYearRound(assessmentRoundId);

        Map<Long, List<VisualWeightedScore>> weightedByUserId = weightedScores.stream()
                .collect(Collectors.groupingBy(
                        w -> w.getUserYearRound().getUser().getId())
                );

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            buildQualitativeAnswersXlsx(
                    wb,
                    users,
                    pairsByUserId,
                    responseIndex,
                    surveyCount,
                    surveyContentByNo
            );

            buildWeightedScoresXlsx(
                    wb,
                    users,
                    weightedByUserId
            );

            wb.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to export evaluation excel", e);
        }
    }

    private void buildQualitativeAnswersXlsx(
            Workbook wb,
            List<UserEntity> users,
            Map<Long, List<UserDataIdCodePair>> pairsByUserId,
            Map<Long, Map<Long, Map<Integer, UserSurveyResponsePair>>> responseIndex,
            int surveyCount,
            Map<Integer, String> surveyContentByNo
    ) {
        Sheet sheet = wb.createSheet("qualitative_answers");
        CellStyle headerStyle = createHeaderStyle(wb);

        Row header = sheet.createRow(0);
        int col = 0;

        header.createCell(col).setCellValue("memberId");
        header.getCell(col++).setCellStyle(headerStyle);

        header.createCell(col).setCellValue("memberName");
        header.getCell(col++).setCellStyle(headerStyle);

        header.createCell(col).setCellValue("dataId");
        header.getCell(col++).setCellStyle(headerStyle);

        header.createCell(col).setCellValue("dataCode");
        header.getCell(col++).setCellStyle(headerStyle);

        for (int qNo = 1; qNo <= surveyCount; qNo++) {
            String content = Optional.ofNullable(surveyContentByNo.get(qNo)).orElse("");
            String headerText = "Q" + qNo + (content.isBlank() ? "" : ": " + content);

            header.createCell(col).setCellValue(headerText);
            header.getCell(col++).setCellStyle(headerStyle);
        }

        int r = 1;

        for (UserEntity user : users) {
            List<UserDataIdCodePair> pairs = pairsByUserId.getOrDefault(user.getId(), List.of());

            for (UserDataIdCodePair pair : pairs) {
                Row row = sheet.createRow(r++);
                int c = 0;

                row.createCell(c++).setCellValue(nvl(user.getId()));
                row.createCell(c++).setCellValue(nvl(user.getName()));
                row.createCell(c++).setCellValue(nvl(pair.dataId()));
                row.createCell(c++).setCellValue(nvl(pair.dataCode()));

                Map<Integer, UserSurveyResponsePair> bySurveyNo =
                        responseIndex.getOrDefault(user.getId(), Map.of())
                                .getOrDefault(pair.dataId(), Map.of());

                for (int qNo = 1; qNo <= surveyCount; qNo++) {
                    UserSurveyResponsePair resp = bySurveyNo.get(qNo);
                    row.createCell(c++).setCellValue(formatAnswer(resp));
                }
            }
        }

        setQualitativeColumnWidths(sheet, surveyCount);
    }

    private void buildWeightedScoresXlsx(
            Workbook wb,
            List<UserEntity> users,
            Map<Long, List<VisualWeightedScore>> weightedByUserId
    ) {
        Sheet sheet = wb.createSheet("weighted_scores");
        CellStyle headerStyle = createHeaderStyle(wb);

        String[] headers = {
                "memberId", "memberName",
                "심미성", "조형성", "독창성", "사용성", "기능성", "윤리성", "경제성", "목적성",
                "category"
        };

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int r = 1;
        for (UserEntity user : users) {
            List<VisualWeightedScore> wsList = weightedByUserId.getOrDefault(user.getId(), Collections.emptyList());

            if (wsList.isEmpty()) {
                Row row = sheet.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(nvl(user.getId()));
                row.createCell(c++).setCellValue(nvl(user.getName()));
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
            } else {
                for (VisualWeightedScore ws : wsList) {
                    Row row = sheet.createRow(r++);
                    int c = 0;

                    row.createCell(c++).setCellValue(nvl(user.getId()));
                    row.createCell(c++).setCellValue(nvl(user.getName()));

                    row.createCell(c++).setCellValue(nvl(ws.getScore1()));
                    row.createCell(c++).setCellValue(nvl(ws.getScore2()));
                    row.createCell(c++).setCellValue(nvl(ws.getScore3()));
                    row.createCell(c++).setCellValue(nvl(ws.getScore4()));
                    row.createCell(c++).setCellValue(nvl(ws.getScore5()));
                    row.createCell(c++).setCellValue(nvl(ws.getScore6()));
                    row.createCell(c++).setCellValue(nvl(ws.getScore7()));
                    row.createCell(c++).setCellValue(nvl(ws.getScore8()));

                    row.createCell(c++).setCellValue(
                            ws.getVisualDataCategory() == null ? "" : ws.getVisualDataCategory().name()
                    );
                }
            }
        }

        setWeightedColumnWidths(sheet);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return headerStyle;
    }

    private void setWeightedColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 12 * 256); // memberId
        sheet.setColumnWidth(1, 18 * 256); // memberName

        for (int i = 2; i <= 9; i++) {
            sheet.setColumnWidth(i, 10 * 256);
        }
        sheet.setColumnWidth(10, 14 * 256); // category
    }

    private void setQualitativeColumnWidths(Sheet sheet, int surveyCount) {
        sheet.setColumnWidth(0, 12 * 256); // memberId
        sheet.setColumnWidth(1, 18 * 256); // memberName
        sheet.setColumnWidth(2, 10 * 256); // dataId
        sheet.setColumnWidth(3, 14 * 256); // dataCode

        // Q1~Qn (answers) columns
        for (int i = 0; i < surveyCount; i++) {
            int col = 4 + i;
            sheet.setColumnWidth(col, 40 * 256);
        }
    }

    private String nvl(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String formatAnswer(UserSurveyResponsePair r) {
        if (r == null) return "";
        String num = (r.numberResponse() == null) ? "" : String.valueOf(r.numberResponse());
        String txt = (r.textResponse() == null || r.textResponse().isBlank()) ? "" : r.textResponse();

        if (!num.isBlank() && !txt.isBlank()) return num + "/" + txt;
        if (!num.isBlank()) return num;
        return txt;
    }

}

