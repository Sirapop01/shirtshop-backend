# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# ดึง dependencies ล่วงหน้า (cache)
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

# คัดลอกซอร์สและ build (จะได้ target/shirtshop-backend.jar เพราะกำหนด finalName แล้ว)
COPY src ./src
RUN mvn -B -DskipTests package

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV JAVA_OPTS="-Xms128m -Xmx384m"
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod
COPY --from=build /app/target/shirtshop-backend.jar app.jar
EXPOSE 8080
CMD ["sh","-c","java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
