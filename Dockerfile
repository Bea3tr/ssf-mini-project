FROM eclipse-temurin:22-jdk-noble AS builder

WORKDIR /src

COPY mvnw .
COPY pom.xml .
COPY src src
COPY .mvn .mvn

RUN chmod a+x ./mvnw && ./mvnw package -Dmaven.test.skip=true

FROM eclipse-temurin:22-jre-noble

WORKDIR /app

COPY --from=builder /src/target/budgetbliss-0.0.1-SNAPSHOT.jar app.jar

RUN apt update && apt install -y curl

ENV PORT=8080
EXPOSE ${PORT}

ENV SPRING_DATA_REDIS_HOST=localhost SPRING_DATA_REDIS_PORT=6379
ENV SPRING_DATA_REDISUSERNAME="" SPRING_DATA_REDIS_PASSWORD=""
ENV CURR_APIKEY="" MY_APIKEY=""
ENV LOCAL_URL=""

HEALTHCHECK --interval=60s --timeout=30s --start-period=120s --retries=3 \
    CMD curl -f -s http://localhost:${PORT}/status || exit 1

ENTRYPOINT SERVER_PORT=${PORT} java -jar app.jar