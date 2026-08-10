# 1단계: jar를 레이어별로 추출
# (의존성 레이어는 코드가 바뀌어도 그대로라, 아래 2단계에서 Docker 레이어 캐시가 재사용됨)
FROM eclipse-temurin:17-jre-alpine AS builder
WORKDIR /workspace
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# 2단계: 추출된 레이어를 의존성 -> 애플리케이션 코드 순으로 복사
# 의존성이 안 바뀌면 여기까지는 캐시 히트되고, 마지막 application 레이어만 새로 빌드/전송됨
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
