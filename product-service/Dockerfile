FROM eclipse-temurin:25.0.2_10-jdk
RUN apt-get -o Acquire::Check-Valid-Until=false -o Acquire::Max-FutureTime=86400 update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/product-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
