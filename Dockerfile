FROM eclipse-temurin:22-jre-jammy
COPY out/artifacts/MycoCode_jar/MycoCode.jar app.jar 
COPY src/main/resources/exemplar.pdf exemplar.pdf
ENTRYPOINT ["java", "-jar", "app.jar"]
