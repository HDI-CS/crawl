package kr.co.hdi.admin.export.service;

import kr.co.hdi.domain.assignment.query.UserDataIdCodePair;
import kr.co.hdi.domain.assignment.repository.IndustryDataAssignmentRepository;
import kr.co.hdi.domain.data.enums.IndustryDataCategory;
import kr.co.hdi.domain.response.query.UserIndustryWeightedScorePair;
import kr.co.hdi.domain.response.query.UserResponsePair;
import kr.co.hdi.domain.response.repository.IndustryResponseRepository;
import kr.co.hdi.domain.response.repository.IndustryWeightedScoreRepository;
import kr.co.hdi.domain.survey.entity.IndustrySurvey;
import kr.co.hdi.domain.survey.enums.SurveyType;
import kr.co.hdi.domain.survey.repository.IndustrySurveyRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndustryResultExportService {

    private final IndustryDataAssignmentRepository assignmentRepository;
    private final IndustryResponseRepository responseRepository;
    private final IndustryWeightedScoreRepository weightedScoreRepository;
    private final IndustrySurveyRepository surveyRepository;

    private static final Set<String> EXCLUDED_EVALUATOR_NAMES = Set.of("TEST_PR");


    private static final String[] DIMENSION_LABELS = {
            "심미성", "조형성", "독창성", "사용성", "기능성", "윤리성", "경제성", "목적성"
    };

    private static final Map<IndustryDataCategory, String> CATEGORY_LABELS = Map.of(
            IndustryDataCategory.WIRELESS_MOUSE, "무선마우스",
            IndustryDataCategory.BLUETOOTH_SPEAKER, "포터블스피커",
            IndustryDataCategory.UMPC, "UMPC",
            IndustryDataCategory.CAMERA, "카메라",
            IndustryDataCategory.WEBCAM, "웹캠",
            IndustryDataCategory.PROJECTOR, "프로젝터"
    );

    public void export(Long assessmentRoundId, Long yearId, String outputPath) {
        List<IndustrySurvey> surveys = surveyRepository.findAllByYear(yearId);

        List<IndustrySurvey> numberSurveys = surveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.NUMBER)
                .sorted(Comparator.comparing(IndustrySurvey::getSurveyNumber))
                .toList();

        IndustrySurvey textSurvey = surveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.TEXT)
                .findFirst()
                .orElse(null);

        // (userId, dataId, surveyCode) -> [numberResponse, textResponse]
        List<UserResponsePair> responsePairs = responseRepository.findPairsByUserYearRound(assessmentRoundId);
        Map<String, UserResponsePair> responseMap = responsePairs.stream()
                .collect(Collectors.toMap(
                        r -> r.userId() + "_" + r.dataId() + "_" + r.surveyCode(),
                        r -> r,
                        (a, b) -> a
                ));

        // userId -> (dataId, originalId) 목록, originalId(코드) 순으로 이미 정렬됨
        List<UserDataIdCodePair> dataPairs = assignmentRepository
                .findDataIdCodePairsByAssessmentRoundId(assessmentRoundId);
        Map<Long, List<UserDataIdCodePair>> dataByUser = dataPairs.stream()
                .collect(Collectors.groupingBy(UserDataIdCodePair::userId, LinkedHashMap::new, Collectors.toList()));

        // userId -> 카테고리별 가중치 목록 (한 명이 여러 카테고리 가능)
        List<UserIndustryWeightedScorePair> weightPairs = weightedScoreRepository
                .findPairsByUserYearRound(assessmentRoundId);
        Map<Long, List<UserIndustryWeightedScorePair>> weightsByUser = weightPairs.stream()
                .collect(Collectors.groupingBy(UserIndustryWeightedScorePair::userId));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (Map.Entry<Long, List<UserDataIdCodePair>> entry : dataByUser.entrySet()) {
                Long userId = entry.getKey();
                List<UserDataIdCodePair> items = entry.getValue();
                String userName = items.get(0).userName();

                if (EXCLUDED_EVALUATOR_NAMES.contains(userName)) {
                    System.out.println("⏭️ 제외: " + userName);
                    continue;
                }

                Sheet sheet = workbook.createSheet(safeSheetName(userName));
                writeEvaluatorSheet(
                        sheet, userId, items, numberSurveys, textSurvey,
                        responseMap, weightsByUser.getOrDefault(userId, List.of())
                );
            }

            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                workbook.write(out);
            }
            System.out.println("✅ 엑셀 생성 완료: " + outputPath);

        } catch (Exception e) {
            throw new RuntimeException("엑셀 생성 실패", e);
        }
    }

    private void writeEvaluatorSheet(
            Sheet sheet, Long userId, List<UserDataIdCodePair> items,
            List<IndustrySurvey> numberSurveys, IndustrySurvey textSurvey,
            Map<String, UserResponsePair> responseMap,
            List<UserIndustryWeightedScorePair> weights
    ) {
        int rowIdx = 0;

        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("ID");
        int col = 1;
        for (IndustrySurvey s : numberSurveys) {
            header.createCell(col++).setCellValue(s.getSurveyNumber() + ". " + s.getSurveyCode());
        }
        header.createCell(col).setCellValue("정성평가");

        for (UserDataIdCodePair item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.dataId());

            int c = 1;
            for (IndustrySurvey s : numberSurveys) {
                UserResponsePair r = responseMap.get(userId + "_" + item.dataId() + "_" + s.getSurveyCode());
                if (r != null && r.numberResponse() != null) {
                    row.createCell(c).setCellValue(r.numberResponse());
                }
                c++;
            }

            if (textSurvey != null) {
                UserResponsePair r = responseMap.get(userId + "_" + item.dataId() + "_" + textSurvey.getSurveyCode());
                if (r != null && r.textResponse() != null) {
                    row.createCell(c).setCellValue(r.textResponse());
                }
            }
        }

        // 카테고리별 가중치평가 블록 (평가자가 담당한 카테고리 수만큼 반복)
        for (UserIndustryWeightedScorePair w : weights) {
            rowIdx++; // 빈 행

            String categoryLabel = CATEGORY_LABELS.getOrDefault(w.industryDataCategory(), w.industryDataCategory().name());

            Row categoryRow = sheet.createRow(rowIdx++);
            categoryRow.createCell(0).setCellValue("카테고리 (" + categoryLabel + ")");
            for (int i = 0; i < DIMENSION_LABELS.length; i++) {
                categoryRow.createCell(i + 1).setCellValue(DIMENSION_LABELS[i]);
            }

            Row weightRow = sheet.createRow(rowIdx);
            weightRow.createCell(0).setCellValue("가중치 평가");
            int[] scores = {
                    w.score1(), w.score2(), w.score3(), w.score4(),
                    w.score5(), w.score6(), w.score7(), w.score8()
            };
            for (int i = 0; i < scores.length; i++) {
                weightRow.createCell(i + 1).setCellValue(scores[i]);
            }
        }
    }

    private String safeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/*?:\\[\\]]", "");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
