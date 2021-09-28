FROM gradle:6.3.0-jdk14 as build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
RUN ./gradlew build
COPY src ./src
RUN ./gradlew shadowJar

FROM openjdk:14-alpine
RUN apk update
RUN apk add python3
EXPOSE 4567
ENTRYPOINT ["java", "-jar", "yt-sync-all.jar"]
