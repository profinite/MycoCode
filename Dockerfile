FROM eclipse-temurin:22-jre-jammy
COPY out/artifacts/MycoCode_jar/MycoCode.jar app.jar 
COPY target/*.jar app.jar

COPY target/blankTags.pdf blankTags.pdf
COPY target/blankTags.pdf /app/blankTags.pdf
COPY target/blankFundis.pdf blankFundis.pdf
COPY target/blankFundis.pdf /app/blankFundis.pdf

# Set the working directory to the application JAR file location
WORKDIR /app

COPY out/artifacts/MycoCode_jar/MycoCode.jar app.jar 
COPY out/artifacts/MycoCode_jar/MycoCode.jar /app/app.jar 

# Expose the application port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
