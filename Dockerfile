# Use a Java 17 JDK base image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy all files into the image
COPY . .

# Ensure mvnw is executable
RUN chmod +x mvnw

# Build the application using Maven Wrapper
RUN ./mvnw clean install -DskipTests

# Expose the port your app runs on
EXPOSE 8080

# Run the built JAR file (update the JAR name if needed)
CMD ["java", "-jar", "target/document_converter-0.0.1-SNAPSHOT.jar"]
