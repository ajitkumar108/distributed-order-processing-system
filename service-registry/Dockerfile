# ---------- Stage 1: BUILD the jar inside a Maven container ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Copy only pom.xml first so Docker can cache the downloaded dependencies between builds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Now copy the source code and build (tests are skipped here; run them with "mvn test" locally).
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- Stage 2: RUN the jar in a small Java runtime image ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
