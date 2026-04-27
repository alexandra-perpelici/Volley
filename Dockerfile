FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

RUN chmod +x mvnw \
    && ./mvnw -DskipTests --no-transfer-progress clean package \
    && jar tf target/Volley_Reservations-0.0.1-SNAPSHOT.jar | grep 'BOOT-INF/lib/postgresql-'

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/Volley_Reservations-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
