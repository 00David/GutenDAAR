# syntax=docker/dockerfile:1

# ---- Build stage: compile the application into an executable jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src

# The Maven repository is kept in a BuildKit cache between builds, so dependencies are only downloaded once
# Tests need a PostgreSQL database: they are run by the CI, not here
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B package -DskipTests \
    && cp target/gutenDAAR-*.jar app.jar

# ---- Runtime stage: only a JRE and the jar ----
FROM eclipse-temurin:21-jre

RUN useradd --system --uid 1001 daar

WORKDIR /app
COPY data/*.csv data/

RUN mkdir -p data/covers \
    && chmod 644 data/*.csv \
    && chown -R daar:daar data

COPY --from=build /build/app.jar app.jar

USER daar
EXPOSE 8080

# The JVM heap is sized relatively to the memory given to the container, instead of the host memory
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
