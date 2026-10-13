# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime stage with lightweight JRE + Built-in MySQL Server
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV DEBIAN_FRONTEND=noninteractive

# Install MySQL Server for all-in-one zero-config deployment on Render
RUN apt-get update && \
    apt-get install -y --no-install-recommends mysql-server mysql-client && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh"]
