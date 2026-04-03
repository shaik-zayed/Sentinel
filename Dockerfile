
FROM maven:3.9.8-eclipse-temurin-21 AS deps
WORKDIR /app

COPY pom.xml .

COPY api-gateway/pom.xml    api-gateway/
COPY auth-service/pom.xml   auth-service/
COPY eureka-server/pom.xml  eureka-server/
COPY nmap_service/pom.xml   nmap_service/
COPY report-service/pom.xml report-service/
COPY scan_service/pom.xml   scan_service/


RUN mvn dependency:go-offline -B -q


FROM deps AS builder

COPY . .

ARG SERVICE_NAME
RUN test -n "$SERVICE_NAME" || (echo "ERROR: --build-arg SERVICE_NAME=<module> is required" && exit 1)

RUN mvn clean package \
      -pl "${SERVICE_NAME}" \
      -am \
      -DskipTests \
      -T 1C \
      -B \
      -q


FROM builder AS extractor

ARG SERVICE_NAME

RUN java -Djarmode=tools \
         -jar "${SERVICE_NAME}"/target/*.jar \
         extract \
         --layers \
         --launcher \
         --destination /app/extracted

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

#Installing curl to the image as it does not have it.
RUN apk add --no-cache curl

RUN addgroup -S sentinel && adduser -S sentinel -G sentinel

ENV JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.net.preferIPv4Stack=true \
    -Dfile.encoding=UTF-8"

# Switch to non-root before copying application files
USER sentinel

COPY --from=extractor --chown=sentinel:sentinel /app/extracted/dependencies/          ./
COPY --from=extractor --chown=sentinel:sentinel /app/extracted/spring-boot-loader/    ./
COPY --from=extractor --chown=sentinel:sentinel /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=sentinel:sentinel /app/extracted/application/           ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]