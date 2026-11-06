FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN sh mvnw -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 guali
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER guali
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-XX:+UseSerialGC", "-Duser.timezone=America/Guatemala", "-jar", "app.jar"]