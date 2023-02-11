FROM gradle:7.6.0-jdk17 as build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
RUN ./gradlew build
COPY src ./src
RUN ./gradlew shadowJar

FROM eclipse-temurin:17-jre-alpine
RUN apk update
RUN apk add python3
RUN wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp
COPY --from=build /app/build/libs/yt-sync-all.jar ./yt-sync-all.jar
EXPOSE 4567
ENTRYPOINT ["java", "-jar", "yt-sync-all.jar"]
