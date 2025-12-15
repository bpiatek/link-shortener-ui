# ===================================================================================
# STAGE 1: The "Builder" Stage
# - Uses a full JDK and Maven to build the application.
# - This stage will be discarded and not included in the final image.
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

# Copy only the files needed to download dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies into a separate, cached layer
# This layer only changes if you modify pom.xml
RUN ./mvnw dependency:resolve

# Copy the rest of the source code
COPY src ./src

# Build the application JAR
# This layer only changes if your source code changes
RUN ./mvnw package -DskipTests


# ===================================================================================
# STAGE 2: The "Extractor" Stage
# - Extracts the layers from the JAR file created by the builder.
# ===================================================================================
FROM builder as extractor

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar application.jar

# Use Spring Boot's layertools to extract the application into separate layers
# This creates folders like /dependencies, /spring-boot-loader, /application, etc.
RUN java -Djarmode=layertools -jar application.jar extract


# ===================================================================================
# STAGE 3: The Final Image
# ===================================================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 1. Copy the extracted layers from the extractor stage.
# The order matters for caching!
# 'dependencies' change least often, 'application' changes most often.
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

# 2. Define the Entrypoint
# We use JarLauncher instead of 'java -jar' because the JAR is unpacked.
#
# IMPORTANT:
# - For Spring Boot 3.2+ (Java 21 default): Use "org.springframework.boot.loader.launch.JarLauncher"
# - For Spring Boot < 3.2: Use "org.springframework.boot.loader.JarLauncher"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]