# syntax=docker/dockerfile:1.4
# Etapa de build
FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG CACHEBUST=1
WORKDIR /app

COPY pom.xml .

# small cache-busting layer controlled by build-arg; keep it minimal so
# other useful layers remain cacheable
RUN echo "cachebust=$CACHEBUST" > /tmp/cachebust

COPY src ./src
# Use BuildKit cache mount for Maven local repository to speed up builds
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -Dmaven.test.skip=true package

# Etapa de runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

ARG JAR_FILE=target/rating-0.0.1-SNAPSHOT.jar

# Instalar curl para healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiamos el jar desde la imagen de build
COPY --from=build /app/${JAR_FILE} app.jar

# Si tu app escucha en 8082:
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
