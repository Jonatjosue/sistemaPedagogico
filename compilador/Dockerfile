FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY repo repo
COPY src src
RUN sh mvnw -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jdk
RUN useradd --system --create-home --uid 10001 guali
WORKDIR /app
COPY --from=build /build/target/compilador-*.jar app.jar
RUN chown -R guali:guali /app
USER guali
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx192m", "-XX:+UseSerialGC", "-jar", "app.jar"]