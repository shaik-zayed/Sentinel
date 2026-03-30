# --- STAGE 1: Dependency download (cached as long as POMs don't change) -----
FROM maven:3.9.8-eclipse-temurin-21 AS deps
WORKDIR /app

# Copy the parent POM first
COPY pom.xml .

# Copy each module's POM — Docker caches this layer separately from source code,
# so a source-only change won't re-download the internet.
COPY api-gateway/pom.xml    api-gateway/
COPY auth-service/pom.xml   auth-service/
COPY eureka-server/pom.xml  eureka-server/
COPY nmap_service/pom.xml   nmap_service/
COPY report-service/pom.xml report-service/
COPY scan_service/pom.xml   scan_service/

# Download all dependencies in batch mode without transfer-progress noise.
# NOTE: dependency:go-offline does not resolve every plugin/annotation-processor
# that materialises during 'package'. A small amount of network access may still
# occur on the very first 'mvn clean package' run; subsequent runs are fully cached.
RUN mvn dependency:go-offline -B --no-transfer-progress

# --- STAGE 2: Build the requested service ------------------------------------
FROM deps AS builder

# Copy all source code now that deps are cached
COPY . .

ARG SERVICE_NAME
RUN test -n "$SERVICE_NAME" || (echo "ERROR: --build-arg SERVICE_NAME=<module> is required" && exit 1)

# Build only the requested module (-pl) plus everything it depends on (-am).
# -DskipTests skips unit tests for faster image builds.
RUN mvn clean package \
      -pl "${SERVICE_NAME}" \
      -am \
      -DskipTests \
      --no-transfer-progress

# --- STAGE 3: Extract layers (Spring Boot 3.2+ uses jarmode=tools) -----------
FROM builder AS extractor

ARG SERVICE_NAME
RUN java -Djarmode=tools \
         -jar "${SERVICE_NAME}"/target/*.jar \
         extract \
         --layers \
         --launcher \
         --destination extracted

# --- STAGE 4: Lean runtime image ---------------------------------------------
# Using Java 21 for virtual-thread support (Project Loom).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
# Here we are creating a group named sentinel and user named sentinel as well.
RUN addgroup -S sentinel && adduser -S sentinel -G sentinel
USER sentinel

# Copy layers in order of change frequency (least → most frequent).
# This maximises Docker layer reuse across rebuilds because each layer
# uses content hashing to detect changes.
COPY --from=extractor /app/extracted/dependencies/          ./
COPY --from=extractor /app/extracted/spring-boot-loader/    ./
COPY --from=extractor /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/extracted/application/           ./

# JVM tuning: container-aware memory limits, prefer IPv4 stack,
# enable virtual threads via Spring's scheduler (Java 21 / Project Loom).
#ENV JAVA_OPTS="-XX:+UseContainerSupport -Djava.net.preferIPv4Stack=true"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
# Main thing.
# each layer uses hashing to identify if something has changed or not.