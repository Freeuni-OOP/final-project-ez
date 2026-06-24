# AlgoRythm backend — Spring Boot (Java 21).
# Multi-stage: build the jar with Maven, then run it on a slim JRE so the final
# image stays small (no Maven or build tooling shipped).

# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Resolve dependencies first so this layer is cached unless pom.xml changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Then build the app.
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- run stage ---
FROM eclipse-temurin:21-jre AS run
WORKDIR /app
# Copy only the executable Spring Boot jar from the build stage.
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
