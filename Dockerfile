# Staging and production image: the fat JAR plus the public-domain Bibles, meta database and content.
# Licensed Bible files (NRT) are not in the repository; mount them over /app/databases/bibles when they are.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
# Dependencies in their own layer, so a source change doesn't download them again.
RUN ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon -q buildFatJar

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN useradd --system --no-create-home zerno
COPY --from=build /src/build/libs/zerno-backend-all.jar app.jar
COPY databases/meta/meta.SQLite3 databases/meta/meta.SQLite3
COPY databases/bibles databases/bibles
COPY content content
ENV PORT=8080 \
    BIBLE_META_DB_PATH=/app/databases/meta/meta.SQLite3 \
    BIBLE_FILES_DIR=/app/databases/bibles \
    CONTENT_DIR=/app/content
USER zerno
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
