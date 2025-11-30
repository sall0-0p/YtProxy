# Stage 1: Build the Java application
FROM gradle:8-jdk17 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon -x test

# Stage 2: Runtime environment
FROM eclipse-temurin:17-jre-jammy

# Installs dependencies required by yt-dlp (Python, FFmpeg)
RUN apt-get update && \
    apt-get install -y python3 python3-pip ffmpeg curl && \
    rm -rf /var/lib/apt/lists/*

# Downloads and installs the yt-dlp binary
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp

WORKDIR /app

# Copies the built jar from Stage 1
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Creates volume directory for downloads
RUN mkdir downloads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]