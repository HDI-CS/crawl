package kr.co.hdi.survey.service;

import kr.co.hdi.admin.data.service.IndustryDataService;
import kr.co.hdi.admin.data.service.VisualDataService;
import kr.co.hdi.domain.assignment.entity.IndustryDataAssignment;
import kr.co.hdi.domain.assignment.entity.VisualDataAssignment;
import kr.co.hdi.domain.assignment.repository.IndustryDataAssignmentRepository;
import kr.co.hdi.domain.assignment.repository.VisualDataAssignmentRepository;
import kr.co.hdi.domain.currentSurvey.entity.CurrentIndustryCategory;
import kr.co.hdi.domain.currentSurvey.entity.CurrentSurvey;
import kr.co.hdi.domain.currentSurvey.entity.CurrentVisualCategory;
import kr.co.hdi.domain.currentSurvey.repository.CurrentIndustryCategoryRepository;
import kr.co.hdi.domain.currentSurvey.repository.CurrentSurveyRepository;
import kr.co.hdi.domain.currentSurvey.repository.CurrentVisualCategoryRepository;
import kr.co.hdi.domain.data.entity.IndustryData;
import kr.co.hdi.domain.data.entity.VisualData;
import kr.co.hdi.domain.data.enums.IndustryImageType;
import kr.co.hdi.domain.data.repository.IndustryDataRepository;
import kr.co.hdi.domain.data.repository.VisualDataRepository;
import kr.co.hdi.domain.response.entity.IndustryResponse;
import kr.co.hdi.domain.response.entity.IndustryWeightedScore;
import kr.co.hdi.domain.response.entity.VisualResponse;
import kr.co.hdi.domain.response.entity.VisualWeightedScore;
import kr.co.hdi.domain.response.repository.IndustryResponseRepository;
import kr.co.hdi.domain.response.repository.IndustryWeightedScoreRepository;
import kr.co.hdi.domain.response.repository.VisualResponseRepository;
import kr.co.hdi.domain.response.repository.VisualWeightedScoreRepository;
import kr.co.hdi.domain.survey.entity.IndustrySurvey;
import kr.co.hdi.domain.survey.entity.VisualSurvey;
import kr.co.hdi.domain.survey.enums.SurveyType;
import kr.co.hdi.domain.survey.repository.IndustrySurveyRepository;
import kr.co.hdi.domain.survey.repository.VisualSurveyRepository;
import kr.co.hdi.domain.year.entity.UserYearRound;
import kr.co.hdi.domain.year.entity.Year;
import kr.co.hdi.domain.year.enums.DomainType;
import kr.co.hdi.domain.year.repository.UserYearRoundRepository;
import kr.co.hdi.domain.year.repository.YearRepository;
import kr.co.hdi.global.s3.service.ImageService;
import kr.co.hdi.survey.dto.request.industry.IndustryWeightedScoreRequest;
import kr.co.hdi.survey.dto.request.visual.VisualWeightedScoreRequest;
import kr.co.hdi.survey.dto.response.*;
import kr.co.hdi.survey.dto.request.SurveyResponseRequest;
import kr.co.hdi.survey.dto.response.industry.IndustryWeightedScoreResponse;
import kr.co.hdi.survey.dto.response.visual.VisualWeightedScoreResponse;
import kr.co.hdi.survey.exception.SurveyErrorCode;
import kr.co.hdi.survey.exception.SurveyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kr.co.hdi.survey.exception.SurveyErrorCode.NOT_FOUND_YEAR;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final YearRepository yearRepository;
    private final CurrentSurveyRepository currentSurveyRepository;
    private final UserYearRoundRepository userYearRoundRepository;
    private final CurrentVisualCategoryRepository currentVisualCategoryRepository;
    private final CurrentIndustryCategoryRepository currentIndustryCategoryRepository;

    private final VisualDataRepository visualDataRepository;
    private final VisualSurveyRepository visualSurveyRepository;
    private final VisualResponseRepository visualResponseRepository;
    private final VisualDataAssignmentRepository visualDataAssignmentRepository;

    private final IndustryDataRepository industryDataRepository;
    private final IndustrySurveyRepository industrySurveyRepository;
    private final IndustryResponseRepository industryResponseRepository;
    private final IndustryDataAssignmentRepository industryDataAssignmentRepository;

    private final VisualWeightedScoreRepository visualWeightedScoreRepository;
    private final IndustryWeightedScoreRepository industryWeightedScoreRepository;
    private final ImageService imageService;
    private final VisualDataService visualDataService;
    private final IndustryDataService industryDataService;
    private final SurveyResponseUpsertHelper surveyResponseUpsertHelper;

    /*
    [공통] 현재 평가 정보 조회
     */
    public CurrentSurvey getCurrentSurvey(DomainType type) {

        return currentSurveyRepository.findByDomainType(type)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_CURRENT_SURVEY));
    }

    /*
    평가할 시각 디자인 데이터 리스트 조회
     */
    @Transactional
    public List<SurveyDataPreviewResponse> getAllVisualSurveys(Long userId) {

        // 현재 평가 정보
        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.VISUAL);
        Long assessmentRoundId = currentSurvey.getAssessmentRoundId();
        Long yearId = currentSurvey.getYearId();
        Year year = yearRepository.findById(yearId)
                .orElseThrow(() -> new SurveyException(NOT_FOUND_YEAR));

        // 유저에게 할당된 데이터 리스트 조회
        List<VisualDataAssignment> assignments =
                visualDataAssignmentRepository.findAssignmentsByUserAndAssessmentRound(userId, assessmentRoundId);

        return assignments.stream()
                .map(assignment ->
                        SurveyDataPreviewResponse.toResponseDto(
                                assignment,
                                year.getSurveyCount(),
                                visualDataService.resolveImageUrl(assignment.getVisualData())
                        ))
                .toList();
    }

    /*
    평가할 산업 디자인 데이터 리스트 조회
     */
    @Transactional
    public List<SurveyDataPreviewResponse> getAllIndustrySurveys(Long userId) {

        // 현재 평가 정보
        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.INDUSTRY);
        Long assessmentRoundId = currentSurvey.getAssessmentRoundId();
        Long yearId = currentSurvey.getYearId();
        Year year = yearRepository.findById(yearId)
                .orElseThrow(() -> new SurveyException(NOT_FOUND_YEAR));

        // 유저에게 할당된 데이터 리스트 조회
        List<IndustryDataAssignment> assignments =
                industryDataAssignmentRepository.findAssignmentsByUserAndAssessmentRound(userId, assessmentRoundId);

        return assignments.stream()
                .map(assignment ->
                        SurveyDataPreviewResponse.toResponseDto(
                                assignment,
                                year.getSurveyCount(),
                                industryDataService.resolveIndustryImageUrl(assignment.getIndustryData(), IndustryImageType.FRONT)
                        ))
                .toList();
    }

    /*
    시각 디자인 평가 데이터셋 + 응답 조회
     */
    public VisualSurveyDetailResponse getVisualSurveyDetail(Long dataId, Long userId) {

        // 데이터 조회
        VisualData visualData = visualDataRepository.findById(dataId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA));

        // 데이터 이미지 조회
        String visualDataImage = visualDataService.resolveImageUrl(visualData);

        // 설문 문항 조회
        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.VISUAL);
        List<VisualSurvey> visualSurveys = visualSurveyRepository.findAllByYear(currentSurvey.getYearId());

        // 사용자 평가 참여 정보 조회
        UserYearRound userYearRound =
                userYearRoundRepository.findByAssessmentRoundIdAndUserId(
                                currentSurvey.getAssessmentRoundId(),
                                userId
                        )
                        .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        // 데이터에 대한 응답 조회
        List<VisualResponse> responses = visualResponseRepository.findAllByVisualDataIdAndUserId(dataId, userId);
        Map<Long, VisualResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(r -> r.getVisualSurvey().getId(), r -> r));  // key: visualSurveyId, value: visualResponse

        // 설문 + 응답 dto
        List<NumberSurveyResponse> numberResponses = visualSurveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.NUMBER)
                .map(s -> NumberSurveyResponse.of(s, responseMap.get(s.getId())))
                .toList();

        TextSurveyResponse textResponse = visualSurveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.TEXT)
                .findFirst()
                .map(s -> TextSurveyResponse.of(s, responseMap.get(s.getId())))
                .orElse(null);

        VisualDataAssignment assignment =
                visualDataAssignmentRepository
                        .findByUserYearRoundIdAndVisualDataId(userYearRound.getId(), dataId)
                        .orElseThrow(null);

        return new VisualSurveyDetailResponse(
                VisualDatasetResponse.fromEntity(visualData,visualDataImage),
                new SurveyResponse(
                        visualData.getBrandCode() + "_" + visualData.getSectorCategory(),
                        assignment.isSubmitted(),
                        numberResponses,
                        textResponse
                ));
    }

    /*
    산업 디자인 평가 데이터셋 + 응답 조회
     */
    public IndustrySurveyDetailResponse getIndustrySurveyDetail(Long dataId, Long userId) {
        System.out.println("STEP 1 - userYearRound 조회 완료");

        // 데이터 조회
        IndustryData industryData = industryDataRepository.findById(dataId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA));
        System.out.println("STEP 2 - data 조회 완료");

        // 데이터 이미지 조회
        String detailImagePath = industryDataService.resolveIndustryImageUrl(industryData, IndustryImageType.DETAIL);
        String frontImagePath = industryDataService.resolveIndustryImageUrl(industryData, IndustryImageType.FRONT);
        String sideImagePath = industryDataService.resolveIndustryImageUrl(industryData, IndustryImageType.SIDE);
        String side2ImagePath = industryDataService.resolveIndustryImageUrl(industryData, IndustryImageType.SIDE2);
        String side3ImagePath = industryDataService.resolveIndustryImageUrl(industryData, IndustryImageType.SIDE3);
        System.out.println("STEP 3 - assignment 조회 완료");

        // 설문 문항 조회
        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.INDUSTRY);
        List<IndustrySurvey> industrySurveys = industrySurveyRepository.findAllByYear(currentSurvey.getYearId());

        // 데이터에 대한 응답 조회
        List<IndustryResponse> responses = industryResponseRepository.findAllByIndustryDataIdAndUserId(dataId, userId);
        Map<Long, IndustryResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(r -> r.getIndustrySurvey().getId(), r -> r));  // key: industrySurveyId, value: industryResponse
        System.out.println("STEP 4 - response 조회 완료");

        // 사용자 평가 참여 정보 조회
        UserYearRound userYearRound =
                userYearRoundRepository.findByAssessmentRoundIdAndUserId(
                                currentSurvey.getAssessmentRoundId(),
                                userId
                        )
                        .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        // 설문 + 응답 dto
        List<NumberSurveyResponse> numberResponses = industrySurveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.NUMBER)
                .map(s -> NumberSurveyResponse.of(s, responseMap.get(s.getId())))
                .toList();
        TextSurveyResponse textResponses = industrySurveys.stream()
                .filter(s -> s.getSurveyType() == SurveyType.TEXT)
                .findFirst()
                .map(s -> TextSurveyResponse.of(s, responseMap.get(s.getId())))
                .orElse(null);


        IndustryDataAssignment assignment =
                industryDataAssignmentRepository
                        .findByUserYearRoundIdAndIndustryDataId(userYearRound.getId(), dataId)
                        .orElseThrow(null);

        return  new IndustrySurveyDetailResponse(
                IndustryDataSetResponse.fromEntity(
                        industryData,
                        detailImagePath,
                        frontImagePath,
                        sideImagePath,
                        side2ImagePath,
                        side3ImagePath
                ),
                new SurveyResponse(
                        industryData.getOriginalId() + "_" + industryData.getModelName(),
                        assignment.isSubmitted(),
                        numberResponses,
                        textResponses
                ));
    }

    /*
    시각 디자인 응답 저장
     */
    @Transactional
    public void saveVisualSurveyResponse(Long dataId, Long userId, SurveyResponseRequest request) {

        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.VISUAL);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        VisualDataAssignment assignment = visualDataAssignmentRepository.findByUserYearRoundIdAndVisualDataId(userYearRound.getId(), dataId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA_ASSIGNMENT));

        // 설문 문항은 현재 트랜잭션에서 직접 조회 (아래 생성 분기가 별도 트랜잭션을 타므로,
        // 거기서 얻은 프록시를 나중에 여기서 초기화하려 하면 LazyInitializationException이 날 수 있음)
        VisualSurvey survey = visualSurveyRepository.findById(request.surveyId())
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.SURVEY_NOT_FOUND));

        applyVisualSurveyResponse(userYearRound, assignment, survey, dataId, request);
    }

    /*
    시각 디자인 응답 저장 - 공통 로직
    (호출부에서 currentSurvey/userYearRound/assignment/survey를 이미 조회해둔 경우를 위한 내부 메서드.
     여러 문항을 한 번에 저장하는 saveAllVisualSurvey/saveAllAndSubmitVisualSurvey에서
     문항 수만큼 위 조회를 반복하지 않도록 분리했다.)
     */
    private void applyVisualSurveyResponse(
            UserYearRound userYearRound, VisualDataAssignment assignment,
            VisualSurvey survey, Long dataId, SurveyResponseRequest request
    ) {
        // 응답 조회 (없으면 생성 - 동시 요청으로 중복 row가 생기지 않도록 안전하게 생성)
        VisualResponse visualResponse = visualResponseRepository
                .findByUserYearRoundIdAndVisualSurveyIdAndVisualDataId(
                        userYearRound.getId(),
                        survey.getId(),
                        dataId
                )
                .orElseGet(() -> createVisualResponseSafely(userYearRound, assignment, dataId, survey.getId()));

        // 응답값 갱신
        if (survey.getSurveyType() == SurveyType.NUMBER) {
            visualResponse.updateNumberResponse(request.response());
        } else if (survey.getSurveyType() == SurveyType.TEXT) {
            if (assignment.isSubmitted()) {
                assignment.updateSubmitted(false);
            }
            visualResponse.updateTextResponse(request.textResponse());
        }
        visualResponseRepository.save(visualResponse);
    }

    /*
    VisualResponse 생성 (동시 요청 대비)
    - 먼저 별도 트랜잭션(REQUIRES_NEW)으로 생성을 시도한다.
    - 동시에 다른 요청이 먼저 만들어서 유니크 제약(uk_visualResponse_userYearRound_survey_data)에
      걸리면 DataIntegrityViolationException이 던져지는데, 이땐 그 요청이 만든 row를
      재조회해서 사용한다.
    - catch는 반드시 여기(호출부)에서 해야 한다. surveyResponseUpsertHelper 메서드
      내부에서 잡으면 REQUIRES_NEW 트랜잭션의 지연된 커밋 실패(UnexpectedRollbackException)가
      이 메서드의 바깥 트랜잭션까지 전파되어 정상 요청까지 롤백시킨다.
    */
    private VisualResponse createVisualResponseSafely(
            UserYearRound userYearRound, VisualDataAssignment assignment, Long dataId, Long surveyId
    ) {
        try {
            VisualResponse created = surveyResponseUpsertHelper.createVisualResponse(userYearRound, surveyId, dataId);
            // 이 요청이 실제로 새로 만든 경우에만 카운트 (경쟁에서 진 요청은 중복 카운트하지 않음)
            assignment.incrementResponseCount();
            return created;
        } catch (DataIntegrityViolationException e) {
            log.info("동시 요청으로 인한 VisualResponse 중복 생성 시도 감지 (userYearRoundId={}, surveyId={}, dataId={}) - 재조회로 처리",
                    userYearRound.getId(), surveyId, dataId);
            return visualResponseRepository
                    .findByUserYearRoundIdAndVisualSurveyIdAndVisualDataId(userYearRound.getId(), surveyId, dataId)
                    .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA_ASSIGNMENT));
        }
    }

    /*
시각 디자인 전체 응답 저장 + 제출
*/
    @Transactional
    public void saveAllAndSubmitVisualSurvey(
            Long dataId, Long userId, List<SurveyResponseRequest> requests) {

        // 1. 전체 응답 저장
        saveAllVisualSurveyResponses(dataId, userId, requests);

        // 2. 제출
        submitVisualSurvey(dataId, userId);
    }

    // 임시저장용 (제출 없음)
    @Transactional
    public void saveAllVisualSurvey(
            Long dataId, Long userId, List<SurveyResponseRequest> requests) {

        saveAllVisualSurveyResponses(dataId, userId, requests);
        // submitVisualSurvey 호출 안 함
    }

    /*
    시각 디자인 전체 응답 저장 - 공통 로직
    (currentSurvey/userYearRound/assignment/설문 문항 목록을 요청 개수만큼 반복 조회하지 않도록
     루프 밖에서 한 번만 조회한다.)
     */
    private void saveAllVisualSurveyResponses(Long dataId, Long userId, List<SurveyResponseRequest> requests) {

        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.VISUAL);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        VisualDataAssignment assignment = visualDataAssignmentRepository.findByUserYearRoundIdAndVisualDataId(userYearRound.getId(), dataId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA_ASSIGNMENT));

        Map<Long, VisualSurvey> surveyById = visualSurveyRepository.findAllByYear(currentSurvey.getYearId()).stream()
                .collect(Collectors.toMap(VisualSurvey::getId, s -> s));

        for (SurveyResponseRequest request : requests) {
            VisualSurvey survey = surveyById.get(request.surveyId());
            if (survey == null) {
                throw new SurveyException(SurveyErrorCode.SURVEY_NOT_FOUND);
            }
            applyVisualSurveyResponse(userYearRound, assignment, survey, dataId, request);
        }
    }


    /*
    산업 디자인 응답 저장
     */
    @Transactional
    public void saveIndustrySurveyResponse(Long dataId, Long userId, SurveyResponseRequest request) {

        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.INDUSTRY);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        IndustryDataAssignment assignment = industryDataAssignmentRepository.findByUserYearRoundIdAndIndustryDataId(userYearRound.getId(), dataId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA_ASSIGNMENT));

        // 설문 문항은 현재 트랜잭션에서 직접 조회 (아래 생성 분기가 별도 트랜잭션을 타므로,
        // 거기서 얻은 프록시를 나중에 여기서 초기화하려 하면 LazyInitializationException이 날 수 있음)
        IndustrySurvey survey = industrySurveyRepository.findById(request.surveyId())
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.SURVEY_NOT_FOUND));

        applyIndustrySurveyResponse(userYearRound, assignment, survey, dataId, request);
    }

    /*
    산업 디자인 응답 저장 - 공통 로직
    (호출부에서 currentSurvey/userYearRound/assignment/survey를 이미 조회해둔 경우를 위한 내부 메서드.
     여러 문항을 한 번에 저장하는 saveAllIndustrySurvey/saveAllAndSubmitIndustrySurvey에서
     문항 수만큼 위 조회를 반복하지 않도록 분리했다.)
     */
    private void applyIndustrySurveyResponse(
            UserYearRound userYearRound, IndustryDataAssignment assignment,
            IndustrySurvey survey, Long dataId, SurveyResponseRequest request
    ) {
        // 응답 조회 (없으면 생성 - 동시 요청으로 중복 row가 생기지 않도록 안전하게 생성)
        IndustryResponse industryResponse = industryResponseRepository
                .findByUserYearRoundIdAndIndustrySurveyIdAndIndustryDataId(
                        userYearRound.getId(),
                        survey.getId(),
                        dataId
                )
                .orElseGet(() -> createIndustryResponseSafely(userYearRound, assignment, dataId, survey.getId()));

        // 응답값 갱신
        if (survey.getSurveyType() == SurveyType.NUMBER) {
            industryResponse.updateNumberResponse(request.response());
        } else if (survey.getSurveyType() == SurveyType.TEXT) {
            if (assignment.isSubmitted()) {
                assignment.updateSubmitted(false);
            }
            industryResponse.updateTextResponse(request.textResponse());
        }
        industryResponseRepository.save(industryResponse);
    }

    /*
    IndustryResponse 생성 (동시 요청 대비) - createVisualResponseSafely와 동일한 전략
    */
    private IndustryResponse createIndustryResponseSafely(
            UserYearRound userYearRound, IndustryDataAssignment assignment, Long dataId, Long surveyId
    ) {
        try {
            IndustryResponse created = surveyResponseUpsertHelper.createIndustryResponse(userYearRound, surveyId, dataId);
            // 이 요청이 실제로 새로 만든 경우에만 카운트 (경쟁에서 진 요청은 중복 카운트하지 않음)
            assignment.incrementResponseCount();
            return created;
        } catch (DataIntegrityViolationException e) {
            log.info("동시 요청으로 인한 IndustryResponse 중복 생성 시도 감지 (userYearRoundId={}, surveyId={}, dataId={}) - 재조회로 처리",
                    userYearRound.getId(), surveyId, dataId);
            return industryResponseRepository
                    .findByUserYearRoundIdAndIndustrySurveyIdAndIndustryDataId(userYearRound.getId(), surveyId, dataId)
                    .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA_ASSIGNMENT));
        }
    }

    /*
       산업 디자인 전체 응답 저장
        */
    @Transactional
    public void saveAllAndSubmitIndustrySurvey(
            Long dataId, Long userId, List<SurveyResponseRequest> requests) {

        // 1. 전체 응답 저장
        saveAllIndustrySurveyResponses(dataId, userId, requests);

        // 2. 제출 (기존 메서드 재활용)
        submitIndustrySurvey(dataId, userId);
    }

    // 임시저장용 (제출 없음)
    @Transactional
    public void saveAllIndustrySurvey(Long dataId, Long userId, List<SurveyResponseRequest> requests) {
        saveAllIndustrySurveyResponses(dataId, userId, requests);
        // submitIndustrySurvey 호출 안 함
    }

    /*
    산업 디자인 전체 응답 저장 - 공통 로직
    (currentSurvey/userYearRound/assignment/설문 문항 목록을 요청 개수만큼 반복 조회하지 않도록
     루프 밖에서 한 번만 조회한다.)
     */
    private void saveAllIndustrySurveyResponses(Long dataId, Long userId, List<SurveyResponseRequest> requests) {

        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.INDUSTRY);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        IndustryDataAssignment assignment = industryDataAssignmentRepository.findByUserYearRoundIdAndIndustryDataId(userYearRound.getId(), dataId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA_ASSIGNMENT));

        Map<Long, IndustrySurvey> surveyById = industrySurveyRepository.findAllByYear(currentSurvey.getYearId()).stream()
                .collect(Collectors.toMap(IndustrySurvey::getId, s -> s));

        for (SurveyResponseRequest request : requests) {
            IndustrySurvey survey = surveyById.get(request.surveyId());
            if (survey == null) {
                throw new SurveyException(SurveyErrorCode.SURVEY_NOT_FOUND);
            }
            applyIndustrySurveyResponse(userYearRound, assignment, survey, dataId, request);
        }
    }

    /*
    시각 디자인 가중치 평가 조회
    - 만약 현재 차수에 응답한 가중치 평가가 없으면 생성해서 반환
     */
    @Transactional
    public List<VisualWeightedScoreResponse> getVisualWeightedResponse(Long userId) {

        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.VISUAL);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        List<VisualWeightedScore> visualWeightedScores = visualWeightedScoreRepository.findAllByUserYearRoundId(userYearRound.getId());

        // 만약 조회된 가중치 평가가 없다면 해당 차수에 처음 가중치 평가를 하는 것
        // CurrentVisualCategory에 대해서 가중치 평가 빈 응답을 만들어서 반환
        if (visualWeightedScores.isEmpty()) {

            List<CurrentVisualCategory> categories = currentVisualCategoryRepository.findAll();

            visualWeightedScores = visualWeightedScoreRepository.saveAll(
                    categories.stream()
                            .map(category ->
                                    VisualWeightedScore.create(userYearRound, category.getCategory()))
                            .toList()
            );
        }

        return visualWeightedScores.stream()
                .map(VisualWeightedScoreResponse::fromEntity)
                .toList();
    }

    /*
    산업 디자인 가중치 평가 조회
    - 만약 현재 차수에 응답한 가중치 평가가 없으면 생성해서 반환
     */
    @Transactional
    public List<IndustryWeightedScoreResponse> getIndustryWeightedResponse(Long userId) {

        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.INDUSTRY);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        List<IndustryWeightedScore> industryWeightedScores = industryWeightedScoreRepository.findAllByUserYearRoundId(userYearRound.getId());

        // 만약 조회된 가중치 평가가 없다면 해당 차수에 처음 가중치 평가를 하는 것
        // CurrentIndustryCategory에 대해서 가중치 평가 빈 응답을 만들어서 반환
        if (industryWeightedScores.isEmpty()) {

            List<CurrentIndustryCategory> categories = currentIndustryCategoryRepository.findAll();

            industryWeightedScores = industryWeightedScoreRepository.saveAll(
                    categories.stream()
                            .map(category ->
                                    IndustryWeightedScore.create(userYearRound, category.getCategory()))
                            .toList()
            );
        }

        return industryWeightedScores.stream()
                .map(IndustryWeightedScoreResponse::fromEntity)
                .toList();
    }

    /*
    시각 디자인 가중치 평가 저장
     */
    @Transactional
    public void saveVisualWeightedResponse(VisualWeightedScoreRequest request) {

        Long id = request.id();
        VisualWeightedScore visualWeightedScore = visualWeightedScoreRepository.findById(id)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_WEIGHTED_SCORE));

        visualWeightedScore.updateScore(
                request.score1(),
                request.score2(),
                request.score3(),
                request.score4(),
                request.score5(),
                request.score6(),
                request.score7(),
                request.score8()
        );
        visualWeightedScoreRepository.save(visualWeightedScore);
    }

    /*
    산업 디자인 가중치 평가 저장
     */
    @Transactional
    public void saveIndustryWeightedResponse(IndustryWeightedScoreRequest request) {

        Long id = request.id();
        IndustryWeightedScore industryWeightedScore = industryWeightedScoreRepository.findById(id)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_WEIGHTED_SCORE));

        industryWeightedScore.updateScore(
                request.score1(),
                request.score2(),
                request.score3(),
                request.score4(),
                request.score5(),
                request.score6(),
                request.score7(),
                request.score8()
        );
        industryWeightedScoreRepository.save(industryWeightedScore);
    }

    // 브랜드 응답 최종 제출
    @Transactional
    public void submitVisualSurvey(Long dataId, Long userId) {
        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.VISUAL);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        VisualDataAssignment assignment =
                visualDataAssignmentRepository
                        .findByUserYearRoundIdAndVisualDataId(
                                userYearRound.getId(),
                                dataId
                        )
                        .orElseThrow(() -> new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE));

        // 다 작성했는지 체크
//        if (assignment.getResponseCount() < assignment.getSurveyCount()) {
//            throw new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE);
//        }
        long completedResponseCount = visualSurveyRepository.countCompletedResponses(
                userYearRound.getId(),
                dataId
        );
        if (completedResponseCount < assignment.getSurveyCount()) {
            throw new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE);
        }

        // 제출 처리
        assignment.updateSubmitted(true);
        }

    // 제품 응답 최종 제출
    @Transactional
    public void submitIndustrySurvey(Long dataId, Long userId) {
        CurrentSurvey currentSurvey = getCurrentSurvey(DomainType.INDUSTRY);
        UserYearRound userYearRound = userYearRoundRepository.findByAssessmentRoundIdAndUserId(currentSurvey.getAssessmentRoundId(), userId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_USER_YEAR_ROUND));

        IndustryDataAssignment assignment =
                industryDataAssignmentRepository
                        .findByUserYearRoundIdAndIndustryDataId(
                                userYearRound.getId(),
                                dataId
                        )
                        .orElseThrow(() -> new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE));

        // 다 작성했는지 체크
//        if (assignment.getResponseCount() < assignment.getSurveyCount()) {
//            throw new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE);
//        }

        long completedResponseCount = industryResponseRepository.countCompletedResponses(
                userYearRound.getId(),
                dataId
        );
        if (completedResponseCount < assignment.getSurveyCount()) {
            throw new SurveyException(SurveyErrorCode.INCOMPLETE_RESPONSE);
        }


        // 제출 처리
        assignment.updateSubmitted(true);
        }
    }
