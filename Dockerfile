FROM eclipse-temurin:21-jre
WORKDIR /app
ARG APP_JAR=build/libs/tolmol-0.1.0.jar
COPY --chown=10001:10001 ${APP_JAR} /app/tolmol.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/tolmol.jar"]
