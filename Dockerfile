
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Cache dependencies separately from source code
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src

# Build executable Spring Boot JAR
RUN mvn -B -ntp clean package -DskipTests \
    && test -n "$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)"

# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install curl for Docker healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user
RUN useradd --system --uid 10001 --create-home appuser

# Copy the executable JAR
COPY --from=build --chown=appuser:appuser \
    /app/target/*.jar /app/app.jar

# Set Vietnam timezone (UTC+7)
ENV TZ=Asia/Ho_Chi_Minh

USER appuser

# Default Spring Boot port
EXPOSE 8081

ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "/app/app.jar"]
