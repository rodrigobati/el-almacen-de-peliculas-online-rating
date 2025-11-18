# ====== STAGE 1: Build ======
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiamos solo el pom primero para aprovechar cache
COPY pom.xml .

# Descargamos dependencias (sin compilar todavía)
RUN mvn -q -DskipTests dependency:go-offline

# Ahora copiamos el código fuente
COPY src ./src

# Empaquetamos la app (saldrá un .jar en target/)
RUN mvn -q -DskipTests package


# ====== STAGE 2: Runtime ======
FROM eclipse-temurin:21-jre

WORKDIR /app

# Nombre del jar generado por Maven
ARG JAR_FILE=target/rating-0.0.1-SNAPSHOT.jar

# Copiamos el jar desde la imagen de build
COPY --from=build /app/${JAR_FILE} app.jar
COPY scripts/wait-for.sh ./wait-for.sh
RUN chmod +x ./wait-for.sh

# Puerto interno de la app (ya está escuchando en 8082 según tu log)
EXPOSE 8080

# Activamos el perfil "docker" de Spring
ENV SPRING_PROFILES_ACTIVE=docker

CMD ["./wait-for.sh", "mysql:3306", "--", "java", "-jar", "app.jar"]
