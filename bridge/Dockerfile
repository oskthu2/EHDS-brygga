# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

ARG MODULE=fhir-server

COPY pom.xml ./
COPY mapping-engine/pom.xml mapping-engine/
COPY soap-client/pom.xml soap-client/
COPY fhir-server/pom.xml fhir-server/
COPY ntjp-proxy/pom.xml ntjp-proxy/

RUN mvn dependency:go-offline -q --no-transfer-progress

COPY mapping-engine/src mapping-engine/src
COPY soap-client/src soap-client/src
COPY fhir-server/src fhir-server/src
COPY ntjp-proxy/src ntjp-proxy/src

RUN mvn package -DskipTests -q --no-transfer-progress

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG MODULE=fhir-server
RUN addgroup -S ehds && adduser -S ehds -G ehds
COPY --from=build /build/${MODULE}/target/${MODULE}-*.jar app.jar
USER ehds
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
