FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# roda sem root: se o processo for comprometido, não tem privilégio no contêiner
RUN useradd --system --uid 10001 docmind
COPY --from=build --chown=docmind /app/target/docmind-*.jar app.jar
USER docmind
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
