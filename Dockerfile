FROM openjdk:11-jre-slim
COPY out/artifacts/MycoCode_jar/MycoCode.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
