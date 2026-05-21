FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
ENTRYPOINT ["java", "-Xmx256m", "-Xms256m", "-jar", "app.jar"]
