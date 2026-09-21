ARG BUILD_IMAGE=maven:3.9.11-eclipse-temurin-21
ARG RUNTIME_IMAGE=gcr.io/distroless/java21-debian12:nonroot

FROM ${BUILD_IMAGE} AS build

ARG SERVICE
WORKDIR /workspace
COPY pom.xml ./
COPY services ./services
COPY deploy/container/maven-settings.xml /tmp/maven-settings.xml
RUN mvn -s /tmp/maven-settings.xml -B -pl "services/${SERVICE}" -am -DskipTests package

FROM ${RUNTIME_IMAGE}

ARG SERVICE
WORKDIR /app
COPY --from=build "/workspace/services/${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar" app.jar
USER 65532
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
