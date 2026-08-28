FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY runner.Dockerfile runner.Dockerfile
COPY sandbox-proxy.Dockerfile sandbox-proxy.Dockerfile
COPY sandbox-proxy sandbox-proxy
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
