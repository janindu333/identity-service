FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/identityservice-0.0.1-SNAPSHOT.jar app.jar

# docker profile listens on 8080; k8s profile uses 8082 (see application-k8s.properties)
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
