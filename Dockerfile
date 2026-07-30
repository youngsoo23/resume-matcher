# ---- Build stage ----
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon bootJar

# ---- Run stage ----
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
