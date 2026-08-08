# 설문 응답 동시 저장 시 중복 row가 생길 수 있는 Race Condition 수정

## 배경

기존에는 정성평가(서술형 응답)를 글자 하나 입력할 때마다 즉시 저장하는 실시간 저장 방식이었고,
이 시기에 "가끔 정성평가가 저장이 안 된다"는 문의가 간간이 있었다. 이후 실시간 저장 방식을
버리고, 사용자가 "제출" 버튼을 눌러야 한 번에 전체 응답을 저장하는 방식(`save-all` /
`submit-all`)으로 구조를 바꿨다.

구조를 바꾼 뒤 코드 전체를 다시 훑어보다가, 응답 저장 로직 자체에 동시 요청에 취약한
race condition이 남아있는 걸 발견해서 정리한다.

## 문제 코드

`SurveyService.saveVisualSurveyResponse` (산업 디자인 쪽도 동일한 구조):

```java
// 응답 조회 (없으면 생성)
VisualResponse visualResponse = visualResponseRepository
        .findByUserYearRoundIdAndVisualSurveyIdAndVisualDataId(
                userYearRound.getId(),
                request.surveyId(),
                dataId
        )
        .orElseGet(() -> {
            assignment.incrementResponseCount();

            return VisualResponse.builder()
                    .userYearRound(userYearRound)
                    .visualSurvey(visualSurveyRepository.getReferenceById(request.surveyId()))
                    .visualData(visualDataRepository.getReferenceById(dataId))
                    .build();
        });
```

전형적인 **check-then-act** 패턴이다. "이 문항에 대한 응답이 있는지 조회 → 없으면 새로
만든다"는 흐름인데, 이 조회와 생성 사이에 원자성이 없다. 그리고 결정적으로,
`VisualResponse`/`IndustryResponse` 엔티티에는 `(userYearRound, survey, data)` 조합에 대한
**DB 유니크 제약이 걸려있지 않았다.**

즉 아래와 같은 상황이 가능하다:

1. 더블클릭, 네트워크 재시도, 같은 설문을 두 탭에서 열어놓고 저장 등으로 같은 문항에 대한
   저장 요청이 거의 동시에 두 번 들어온다.
2. 두 요청이 모두 `findByUserYearRoundIdAndVisualSurveyIdAndVisualDataId(...)`로 조회했을 때
   서로의 커밋을 아직 보지 못한 채 "없음"을 리턴받는다.
3. 둘 다 `orElseGet`으로 진입해서 각각 새 `VisualResponse` row를 INSERT한다.
4. 같은 문항에 대해 row가 2개 생긴다.

응답 조회 시엔 `Map`에 마지막 것만 덮어써져서 겉으론 멀쩡해 보이지만, 실제로는 오래된
(또는 비어있는) 응답 row가 남아 있다가 어떤 시점엔 그게 조회되면서 "분명 입력했는데
사라졌다"는 식으로 나타날 수 있다.

## 해결 방법

### 1) DB 유니크 제약 추가

```java
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_visualResponse_userYearRound_survey_data",
                columnNames = {
                        "user_year_round_user_year_round_id",
                        "visual_survey_visual_survey_id",
                        "visual_data_visual_data_id"
                }
        )
)
public class VisualResponse extends BaseTimeEntityWithDeletion { ... }
```

`IndustryResponse`에도 동일하게 `(userYearRound, industrySurvey, industryData)` 조합으로
유니크 제약을 추가했다.

### 2) 생성을 별도 트랜잭션(REQUIRES_NEW)으로 분리

PostgreSQL은 트랜잭션 안에서 제약 위반 등 SQL 에러가 한 번 나면, 그 트랜잭션 전체가
abort 상태가 되어 이후 어떤 쿼리도 거부한다 (`current transaction is aborted, commands
ignored until end of transaction block`). 그래서 실패할 수도 있는 INSERT 시도를
`REQUIRES_NEW`로 분리된 트랜잭션에서 실행해야, 경쟁에서 진 요청이 "그 INSERT 트랜잭션만"
롤백하고 원래 트랜잭션은 계속 이어갈 수 있다.

```java
@Component
@RequiredArgsConstructor
public class SurveyResponseUpsertHelper {

    private final VisualResponseRepository visualResponseRepository;
    private final VisualSurveyRepository visualSurveyRepository;
    private final VisualDataRepository visualDataRepository;

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
}
```

호출부에서는 유니크 제약 위반(`DataIntegrityViolationException`)을 잡아서, 경쟁에서
이긴 요청이 만든 row를 재조회해서 쓴다.

```java
private VisualResponse createVisualResponseSafely(
        UserYearRound userYearRound, VisualDataAssignment assignment, Long dataId, Long surveyId
) {
    try {
        VisualResponse created = surveyResponseUpsertHelper.createVisualResponse(userYearRound, surveyId, dataId);
        assignment.incrementResponseCount(); // 실제로 새로 만든 경우에만 카운트
        return created;
    } catch (DataIntegrityViolationException e) {
        log.info("동시 요청으로 인한 VisualResponse 중복 생성 시도 감지 - 재조회로 처리");
        return visualResponseRepository
                .findByUserYearRoundIdAndVisualSurveyIdAndVisualDataId(userYearRound.getId(), surveyId, dataId)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.NOT_FOUND_DATA_ASSIGNMENT));
    }
}
```

## 여기서 삽질한 부분: catch 위치가 잘못되면 더 큰 사고가 난다

처음엔 `try-catch`를 `REQUIRES_NEW` 메서드 **몸통 안**에 넣었었다.

```java
// (첫 번째 시도 - 문제가 있던 버전)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public VisualResponse tryCreateVisualResponse(...) {
    try {
        return visualResponseRepository.saveAndFlush(...);
    } catch (DataIntegrityViolationException e) {
        return null; // 여기서 잡고 정상 반환
    }
}
```

동시 요청 4개로 재현 테스트를 돌려봤더니, 유니크 제약 자체는 잘 작동해서 row는 정확히
1개로 유지됐다. 그런데:

```
요청 4개 중 성공 1 / 실패 3
생성된 visual_response row 개수: 1
```

4개 중 **3개가 `Transaction silently rolled back because it has been marked as
rollback-only` 예외로 실패**했다. 데이터 중복은 안 생겼지만, 경쟁에서 진 요청들이
정상적으로 fallback(재조회 후 업데이트)하지 못하고 그냥 500 에러로 죽어버린 것이다.

**원인:** `saveAndFlush()`가 던진 예외를 메서드 안에서 잡고 `null`을 정상 반환하면,
Spring의 트랜잭션 인터셉터 입장에서는 "예외 없이 정상 종료됐네" 하고 커밋을 시도한다.
그런데 Hibernate는 flush 실패 시점에 이미 그 영속성 컨텍스트(세션)를 "rollback-only"로
내부적으로 마킹해둔 상태라, 커밋 시도 자체가 실패하면서 `UnexpectedRollbackException`이
새로 던져진다. 이 예외는 메서드 몸통 안의 catch로는 잡을 수 없다 — 발생 시점이
"메서드가 리턴한 이후, 프록시가 커밋을 시도하는 순간"이기 때문이다. 그 결과 이 예외가
그대로 호출부(`SurveyService`)까지 전파되고, 호출부의 바깥 트랜잭션까지 롤백 대상으로
마킹되어 정상적인 재조회/업데이트까지 전부 실패해버렸다.

**해결:** catch를 `REQUIRES_NEW` 메서드 몸통이 아니라, **그 메서드를 호출하는 쪽에서
호출 자체를 감싸도록** 옮겼다. 이러면 Spring의 가장 표준적인 경로를 타게 된다 — 예외
발생 → `TransactionInterceptor`가 예외를 감지 → REQUIRES_NEW 트랜잭션을 정상적으로
롤백 → 원본 예외를 그대로 재던짐(rethrow) → 호출부는 (한 번도 오염되지 않은) 자신의
트랜잭션 안에서 그 예외를 캐치해서 재조회로 넘어간다.

```java
// SurveyResponseUpsertHelper - 예외를 그냥 던지게 둔다
@Transactional(propagation = Propagation.REQUIRES_NEW)
public VisualResponse createVisualResponse(UserYearRound userYearRound, Long surveyId, Long dataId) {
    return visualResponseRepository.saveAndFlush(...);
}

// SurveyService - 호출 자체를 try-catch로 감싼다
try {
    VisualResponse created = surveyResponseUpsertHelper.createVisualResponse(userYearRound, surveyId, dataId);
    ...
} catch (DataIntegrityViolationException e) {
    // 재조회
}
```

## 검증

같은 문항에 동시 요청 4개를 쏴서 결과 row 개수를 확인하는 재현용 러너
(`DuplicateResponseReproRunner`, profile: `repro-duplicate-response`)를 만들어서 검증했다.

```java
for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
    int score = (i % 5) + 1;
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
        } finally {
            doneGate.countDown();
        }
    });
}
```

수정 후 실행 결과:

```
▶ 4개 동시 요청 발사 (userId=105, dataId=2088, surveyId=70)
동시 요청으로 인한 VisualResponse 중복 생성 시도 감지 (userYearRoundId=169, surveyId=70, dataId=2088) - 재조회로 처리
동시 요청으로 인한 VisualResponse 중복 생성 시도 감지 (userYearRoundId=169, surveyId=70, dataId=2088) - 재조회로 처리
동시 요청으로 인한 VisualResponse 중복 생성 시도 감지 (userYearRoundId=169, surveyId=70, dataId=2088) - 재조회로 처리
========================================
요청 4개 중 성공 4 / 실패 0
생성된 visual_response row 개수: 1
✅ 중복 없음 (fix 적용됨)
```

4개 모두 성공했고, 그중 3개는 경쟁에서 져서 재조회 경로를 탔지만 에러 없이 정상적으로
값을 업데이트했다. DB에 직접 쿼리해서도 확인:

```sql
SELECT visual_response_id, user_year_round_user_year_round_id AS user_year_round_id,
       visual_data_visual_data_id AS visual_data_id,
       visual_survey_visual_survey_id AS visual_survey_id,
       number_response, created_at, updated_at
FROM visual_response
WHERE user_year_round_user_year_round_id = 169
  AND visual_data_visual_data_id = 2088
  AND visual_survey_visual_survey_id = 70;
```

```
 visual_response_id | user_year_round_id | visual_data_id | visual_survey_id | number_response |         created_at         |         updated_at
--------------------+--------------------+-----------------+-------------------+------------------+----------------------------+----------------------------
             141502 |                169 |            2088 |               70 |               3 | 2026-08-09 03:04:32.128918 | 2026-08-09 03:04:32.207843
(1 row)
```

동시 요청 4개가 들어와도 row는 정확히 1개만 남는다.

## 배포 시 주의할 점

- 이 프로젝트는 Flyway/Liquibase 없이 `spring.jpa.hibernate.ddl-auto: update`를 쓰고
  있어서, 유니크 제약이 배포 시 자동으로 `ALTER TABLE`로 적용된다. **기존 데이터에
  이미 중복 row가 있으면 이 ALTER TABLE 자체가 실패**할 수 있으므로, 배포 전
  아래 쿼리로 기존 중복 여부를 먼저 확인해야 한다.

```sql
SELECT user_year_round_user_year_round_id, visual_survey_visual_survey_id, visual_data_visual_data_id, COUNT(*)
FROM visual_response
GROUP BY 1, 2, 3
HAVING COUNT(*) > 1;
```

- `REQUIRES_NEW`는 요청 하나당 최악의 경우 커넥션을 2개(바깥 트랜잭션 + REQUIRES_NEW
  트랜잭션)까지 동시에 사용할 수 있다. HikariCP 기본 풀 크기(10)를 기준으로, 재현
  테스트에서 동시 요청 수를 너무 크게 잡으면(예: 10개) 커넥션 풀이 고갈되어 무관한
  요청까지 타임아웃날 수 있다 (`Connection is not available, request timed out`).
  실제 운영 트래픽에서는 완전히 동일한 문항에 여러 명이 정확히 같은 순간 몰릴 확률이
  낮고, REQUIRES_NEW 구간 자체도 짧아서 문제될 가능성은 낮지만, 이 패턴을 다른
  곳에도 넓게 쓰게 된다면 풀 크기를 같이 고려해야 한다.

## 요약

| 항목 | 내용 |
|---|---|
| 증상 | 동시 저장 요청 시 같은 문항에 응답 row가 중복 생성될 수 있음 (check-then-act race) |
| 원인 | `VisualResponse`/`IndustryResponse`에 유니크 제약이 없는 상태로 "조회 후 없으면 생성" 패턴을 사용 |
| 1차 해결 시도의 함정 | REQUIRES_NEW 메서드 내부에서 예외를 캐치하면, 지연된 커밋 실패(`UnexpectedRollbackException`)가 캐치를 우회해 바깥 트랜잭션까지 롤백시킴 |
| 최종 해결 | DB 유니크 제약 + REQUIRES_NEW 트랜잭션으로 생성 시도, 예외 캐치는 반드시 호출부에서 |
| 검증 | 동시 요청 4개 재현 테스트 → 성공 4/4, row 1개로 수렴 확인 (DB 직접 조회로 재확인) |
