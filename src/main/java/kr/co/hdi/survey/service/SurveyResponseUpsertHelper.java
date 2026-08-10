package kr.co.hdi.survey.service;

import kr.co.hdi.domain.data.repository.IndustryDataRepository;
import kr.co.hdi.domain.data.repository.VisualDataRepository;
import kr.co.hdi.domain.response.entity.IndustryResponse;
import kr.co.hdi.domain.response.entity.VisualResponse;
import kr.co.hdi.domain.response.repository.IndustryResponseRepository;
import kr.co.hdi.domain.response.repository.VisualResponseRepository;
import kr.co.hdi.domain.survey.repository.IndustrySurveyRepository;
import kr.co.hdi.domain.survey.repository.VisualSurveyRepository;
import kr.co.hdi.domain.year.entity.UserYearRound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설문 응답(VisualResponse/IndustryResponse) row 생성 시 동시 요청으로 인한
 * 중복 row 생성을 막기 위한 헬퍼.
 *
 * "조회 후 없으면 생성" 흐름은 동시에 두 요청이 들어오면 둘 다 "없음"을 보고
 * 각각 insert를 시도할 수 있다 (check-then-act race). 이를 막기 위해
 * VisualResponse/IndustryResponse 에 (userYearRound, survey, data) 조합의
 * DB 유니크 제약을 걸어두고, 이 클래스에서 생성을 별도 트랜잭션(REQUIRES_NEW)으로
 * 시도한다.
 *
 * REQUIRES_NEW를 쓰는 이유: PostgreSQL은 트랜잭션 내에서 한 번 제약 위반 에러가
 * 나면 그 트랜잭션 전체가 abort 상태가 되어 이후 쿼리를 거부한다. 생성 시도를
 * 별도 트랜잭션으로 분리해두면, 경쟁에서 진 쪽은 이 트랜잭션만 롤백되고
 * 호출부(SurveyService)의 원래 트랜잭션은 영향을 받지 않아 이어서 재조회할 수 있다.
 *
 * 주의: 실패 시의 catch는 반드시 "이 메서드를 호출하는 쪽"에서 해야 한다
 * (SurveyService의 createXxxResponseSafely 참고). 이 메서드 내부에서 예외를
 * 잡고 정상 반환해버리면, 메서드 몸통은 정상 종료됐다고 판단한 Spring 트랜잭션
 * 인터셉터가 커밋을 시도하다가 (Hibernate가 flush 실패로 세션을 이미 rollback-only로
 * 마킹해놔서) UnexpectedRollbackException을 던지게 되고, 이게 그대로 바깥
 * 트랜잭션까지 뚫고 올라가 정상적인 요청까지 실패시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SurveyResponseUpsertHelper {

    private final VisualResponseRepository visualResponseRepository;
    private final VisualSurveyRepository visualSurveyRepository;
    private final VisualDataRepository visualDataRepository;

    private final IndustryResponseRepository industryResponseRepository;
    private final IndustrySurveyRepository industrySurveyRepository;
    private final IndustryDataRepository industryDataRepository;

    /**
     * 새 VisualResponse 생성을 시도한다.
     * 동시 요청이 먼저 만들어 유니크 제약에 걸리면 DataIntegrityViolationException을
     * 그대로 던진다 (호출부에서 잡아서 재조회하도록).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VisualResponse createVisualResponse(UserYearRound userYearRound, Long surveyId, Long dataId) {
        return visualResponseRepository.saveAndFlush(
                VisualResponse.builder()
                        .userYearRound(userYearRound)
                        .visualSurvey(visualSurveyRepository.getReferenceById(surveyId))
                        .visualData(visualDataRepository.getReferenceById(dataId))
                        .build()
        );
    }

    /**
     * 새 IndustryResponse 생성을 시도한다.
     * 동시 요청이 먼저 만들어 유니크 제약에 걸리면 DataIntegrityViolationException을
     * 그대로 던진다 (호출부에서 잡아서 재조회하도록).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IndustryResponse createIndustryResponse(UserYearRound userYearRound, Long surveyId, Long dataId) {
        return industryResponseRepository.saveAndFlush(
                IndustryResponse.builder()
                        .userYearRound(userYearRound)
                        .industrySurvey(industrySurveyRepository.getReferenceById(surveyId))
                        .industryData(industryDataRepository.getReferenceById(dataId))
                        .build()
        );
    }
}
