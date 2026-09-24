# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache des dépendances Maven
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S honeylms && adduser -S honeylms -G honeylms
COPY --from=build /app/target/honeylms-backend-*.jar app.jar
RUN chown honeylms:honeylms app.jar
USER honeylms

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
