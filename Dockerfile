FROM eclipse-temurin:21-jdk
WORKDIR /app

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} toget.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "toget.jar"]