FROM eclipse-temurin:22-jre-jammy
COPY out/artifacts/MycoCode_jar/MycoCode.jar app.jar 
ENTRYPOINT ["java", "-jar", "app.jar"]
