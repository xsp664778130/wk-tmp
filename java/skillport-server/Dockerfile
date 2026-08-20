FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY skillport-protocol ./skillport-protocol
COPY skillport-server ./skillport-server
COPY skillport-bridge ./skillport-bridge
RUN mvn -q -pl skillport-server -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 skillport && mkdir -p /app/data/skills && chown -R skillport:skillport /app
COPY --from=builder /workspace/skillport-server/target/skillport-server-1.0.0-SNAPSHOT.jar /app/skillport-server.jar
USER skillport
EXPOSE 8080 9091
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/skillport-server.jar"]
