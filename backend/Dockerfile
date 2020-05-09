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
RUN ln -s /usr/bin/python3 /usr/bin/python
RUN wget https://yt-dl.org/downloads/latest/youtube-dl -O /usr/local/bin/youtube-dl
RUN chmod a+rx /usr/local/bin/youtube-dl
COPY --from=build /app/build/libs/yt-sync-all.jar ./yt-sync-all.jar
EXPOSE 4567
ENTRYPOINT ["java", "-jar", "yt-sync-all.jar"]
