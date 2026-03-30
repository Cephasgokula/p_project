FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY gradlew .
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
# Non-root user
RUN addgroup -S lendiq && adduser -S lendiq -G lendiq
USER lendiq
EXPOSE 8080
ENTRYPOINT ["java", "-jar", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "app.jar"]
