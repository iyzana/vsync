FROM gradle:6.3.0-jdk14

ENV VERSION 0.0.1-SNAPSHOT

WORKDIR /app
COPY . .

#caching
RUN ./gradlew assemble

FROM openjdk:14-alpine

COPY --from=build /app/build/libs/backend-${VERSION}.jar ./backend.jar
EXPOSE 4567

ENTRYPOINT ["java", "-jar", "backend.jar"]
