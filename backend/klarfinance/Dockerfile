# Menggunakan Alpine JRE 21 yang sangat ringan dan hemat storage
FROM eclipse-temurin:21-jre-alpine

# Set direktori kerja di dalam container
WORKDIR /app

# Meng-copy JAR hasil build ke dalam container
# Nama JAR disesuaikan dengan artifactId dan version di pom.xml
COPY target/api-auth-hizkia-0.0.1-SNAPSHOT.jar app.jar

# Expose port default Spring Boot
EXPOSE 8080

# Eksekusi JAR saat container berjalan
ENTRYPOINT ["java", "-jar", "app.jar"]