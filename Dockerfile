# Railway 배포용 이미지: Spring Boot(8080 또는 $PORT) + 법령 조회 파이썬 서버(컨테이너 내부 127.0.0.1:8090)

# 1) 빌드: Gradle로 bootJar 생성 (테스트는 CI/로컬에서)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 2) 실행: JRE 21 + python3
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 \
 && rm -rf /var/lib/apt/lists/*
ENV TZ=Asia/Seoul
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
COPY ai ai
COPY AI_DATA/chunks.jsonl AI_DATA/chunks.jsonl
COPY docker/start.sh start.sh
EXPOSE 8080
CMD ["sh", "start.sh"]
