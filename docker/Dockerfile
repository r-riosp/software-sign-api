# --- builder ---
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# 1) Cache de deps
COPY gradlew gradlew
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
COPY settings.gradle* build.gradle* gradle.properties* ./
RUN ./gradlew dependencies --no-daemon || true

# 2) Resto do projeto (SOBRESCREVE gradlew)
COPY . .

# 3) Reaplicar fix no gradlew (importante!)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 4) Build
RUN ./gradlew clean bootJar --no-daemon --rerun-tasks -x test

# --- runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=/app/build/libs/*.jar
COPY --from=builder ${JAR_FILE} /app/app.jar
EXPOSE 1700
ENTRYPOINT ["java","-jar","/app/app.jar"]