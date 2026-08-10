package kr.co.hdi.admin.repro.runner;

import kr.co.hdi.domain.response.entity.VisualResponse;
import kr.co.hdi.domain.response.repository.VisualResponseRepository;
import kr.co.hdi.survey.dto.request.SurveyResponseRequest;
import kr.co.hdi.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * [재현/검증 전용] 같은 설문 문항에 대해 동시에 여러 번 저장 요청을 보냈을 때
 * VisualResponse row가 중복 생성되는지 확인하는 러너.
 * (fix/duplicate-survey-response 브랜치 작업 검증용 - 병합 전 삭제해도 무방)
 *
 * 사용법
 *  1) 아래 USER_ID / DATA_ID / SURVEY_ID를 실제 DB 값으로 채운다.
 *     - USER_ID   : 현재 평가 회차에 참여 중인 평가자의 user.id
 *     - DATA_ID   : 그 평가자에게 배정된 visual_data.id
 *     - SURVEY_ID : 현재 연도 NUMBER 타입 visual_survey.id
 *  2) 아래 명령으로 실행 (반드시 테스트용 DB/데이터로만!)
 *       ./gradlew bootRun --args='--spring.profiles.active=repro-duplicate-response'
 *  3) 콘솔 마지막에 찍히는 "생성된 row 개수"를 확인
 *       - fix 적용 전 코드에서 돌리면: 1보다 큰 수가 나올 수 있음 (race 재현)
 *       - fix 적용 후 코드에서 돌리면: 항상 1
 *
 * 실행할 때마다 해당 (user, data, survey) 조합의 기존 응답을 먼저 지우고 시작한다.
 */
@Slf4j
@Component
@Profile("repro-duplicate-response")
@RequiredArgsConstructor
public class DuplicateResponseReproRunner implements CommandLineRunner {

    private static final Long USER_ID = 105L;   // TODO: 실제 user.id로 교체
    private static final Long DATA_ID = 2088L;   // TODO: 실제 visual_data.id로 교체
    private static final Long SURVEY_ID = 70L; // TODO: 실제 visual_survey.id로 교체

    // 주의: tryCreateVisualResponse는 REQUIRES_NEW라서 요청 1개가 최악의 경우
    // 커넥션을 2개(바깥 트랜잭션 + REQUIRES_NEW 트랜잭션)까지 동시에 물 수 있다.
    // HikariCP 기본 풀 크기(10)에 여유를 두고 잡는다 (2 * N < pool size).
    private static final int CONCURRENT_REQUESTS = 4;

    private final SurveyService surveyService;
    private final VisualResponseRepository visualResponseRepository;

    @Override
    public void run(String... args) throws Exception {
        if (USER_ID == 0L || DATA_ID == 0L || SURVEY_ID == 0L) {
            log.error("USER_ID / DATA_ID / SURVEY_ID를 실제 DB 값으로 채운 뒤 다시 실행하세요.");
            return;
        }

        deleteExistingResponses();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            int score = (i % 5) + 1; // 1~5 아무 점수
            executor.submit(() -> {
                try {
                    startGate.await();
                    surveyService.saveVisualSurveyResponse(
                            DATA_ID, USER_ID,
                            new SurveyResponseRequest(SURVEY_ID, score, null)
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.warn("요청 실패: {}", e.getMessage());
                } finally {
                    doneGate.countDown();
                }
            });
        }

        log.info("▶ {}개 동시 요청 발사 (userId={}, dataId={}, surveyId={})",
                CONCURRENT_REQUESTS, USER_ID, DATA_ID, SURVEY_ID);
        startGate.countDown(); // 전부 동시에 출발
        doneGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long rowCount = matchingResponses().size();

        log.info("========================================");
        log.info("요청 {}개 중 성공 {} / 실패 {}", CONCURRENT_REQUESTS, successCount.get(), failureCount.get());
        log.info("생성된 visual_response row 개수: {}", rowCount);
        if (successCount.get() == 0) {
            log.warn("⚠️ 성공한 요청이 0개입니다 - 이 결과는 유효한 테스트가 아닙니다 " +
                    "(커넥션 풀 고갈 등 다른 원인일 가능성이 높음). 위 '요청 실패' 로그를 확인하세요.");
        } else if (rowCount > 1) {
            log.info("❌ 중복 발생 (fix 적용 전 코드)");
        } else {
            log.info("✅ 중복 없음 (fix 적용됨)");
        }
        log.info("========================================");
    }

    private void deleteExistingResponses() {
        List<VisualResponse> existing = matchingResponses();
        if (!existing.isEmpty()) {
            visualResponseRepository.deleteAll(existing);
            log.info("기존 응답 {}건 정리 후 재현 시작", existing.size());
        }
    }

    private List<VisualResponse> matchingResponses() {
        return visualResponseRepository.findAllByVisualDataIdAndUserId(DATA_ID, USER_ID).stream()
                .filter(r -> r.getVisualSurvey().getId().equals(SURVEY_ID))
                .collect(Collectors.toList());
    }
}
