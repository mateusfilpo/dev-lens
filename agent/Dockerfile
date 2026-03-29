FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY ../ ./
RUN ./mvnw -pl agent -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/agent/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]