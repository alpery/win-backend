# Use Java 21 as the base image
FROM eclipse-temurin:21-jdk AS build

# Set the working directory
WORKDIR /app

# Copy the Gradle files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Copy the source code
COPY src ./src

# Build the application
RUN ./gradlew bootJar --no-daemon

# Create a smaller runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables for database connection
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/win_db
ENV SPRING_DATASOURCE_USERNAME=win_user
ENV SPRING_DATASOURCE_PASSWORD=win_pass

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]