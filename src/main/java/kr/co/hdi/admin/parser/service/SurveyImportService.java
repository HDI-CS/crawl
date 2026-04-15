package kr.co.hdi.admin.parser.service;//package kr.co.hdi.admin.parser.service;
//
//import kr.co.hdi.admin.parser.dto.SurveyExcelParser;
//import kr.co.hdi.admin.parser.dto.WaterproofExcelParser;
//import kr.co.hdi.admin.parser.dto.WaterproofRow;
//import kr.co.hdi.admin.survey.dto.request.SurveyQuestionRequest;
//import kr.co.hdi.domain.data.entity.IndustryData;
//import kr.co.hdi.domain.data.enums.IndustryDataCategory;
//import kr.co.hdi.domain.data.repository.IndustryDataRepository;
//import kr.co.hdi.domain.survey.entity.IndustrySurvey;
//import kr.co.hdi.domain.survey.enums.SurveyType;
//import kr.co.hdi.domain.survey.repository.IndustrySurveyRepository;
//import kr.co.hdi.domain.year.entity.Year;
//import kr.co.hdi.domain.year.repository.YearRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.nio.file.Path;
//import java.util.List;
//
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class SurveyImportService {
//
//    private final SurveyExcelParser parser;
//    private final IndustrySurveyRepository repository;
//    private final YearRepository yearRepository;
//
//    public void importSurvey(Path path, Long yearId) {
//
//        // 1. 기존 삭제
//        repository.deleteAllByYearId(yearId);
//
//        // 2. 연도 조회
//        Year year = yearRepository.findById(yearId)
//                .orElseThrow(() -> new RuntimeException("year 없음"));
//
//        // 3. 엑셀 파싱
//        List<SurveyQuestionRequest> rows = parser.parse(path);
//
//        // 4. 변환
//        List<IndustrySurvey> list = rows.stream()
//                .map(req -> {
//                    if (req.surveyCode() == null || req.surveyCode().isBlank()) {
//                        throw new RuntimeException("code 없음");
//                    }
//
//                    IndustrySurvey s = IndustrySurvey.create(req, year);
//
//                    // TEXT 타입 sampleText
//                    if (req.type() == SurveyType.TEXT) {
//                        s.updateSampleText("입력해주세요");
//                    }
//
//                    return s;
//                })
//                .toList();
//
//        // 5. 저장
//        repository.saveAll(list);
//
//        System.out.println("✅ 설문 업로드 완료");
//    }
//}