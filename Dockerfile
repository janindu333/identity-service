# Use a base image containing Java runtime environment
FROM openjdk:17-jdk-alpine

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's jar file
ARG JAR_FILE=target/identity-service-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} identity-service.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "/identity-service.jar"]
