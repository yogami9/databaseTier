FROM maven:3.8-openjdk-11 as build

# Set working directory
WORKDIR /app

# Copy the POM file
COPY pom.xml .

# Download dependencies (this layer can be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/banking-db-service.jar ./app.jar

# Expose the port
EXPOSE 8080

# Set environment variables
ENV MONGODB_URI=mongodb+srv://spicelife576:skiPPer8711@cluster0.pmbmm.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
ENV MONGODB_DATABASE=bankdb

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
