# ============ Étape 1 : Build ============
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache des dépendances Maven
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Compilation
COPY src ./src
RUN mvn -B clean package -DskipTests

# ============ Étape 2 : Runtime ============
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8282

# curl n'existe pas en alpine JRE par défaut : le healthcheck utilise wget (busybox)
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8282}/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]