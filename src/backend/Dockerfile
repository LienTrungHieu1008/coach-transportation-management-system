FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# package runs unit tests; verify (CI/compose.test.yaml) additionally runs PostgreSQL ITs.
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -ntp package -DfailIfNoTests=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/coach-backend-0.1.0-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
