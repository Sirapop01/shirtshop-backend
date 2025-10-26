# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Warm dependency cache
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline --no-transfer-progress

# Build
COPY src ./src
RUN mvn -B -DskipTests package --no-transfer-progress

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+ExitOnOutOfMemoryError" \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

# ต้องมี <finalName>shirtshop-backend</finalName> ใน pom.xml
COPY --from=build /src/target/shirtshop-backend.jar app.jar

EXPOSE 8080
CMD ["sh","-c","java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
