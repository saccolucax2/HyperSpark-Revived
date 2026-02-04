FROM eclipse-temurin:8-jre

WORKDIR /app

COPY target/hyperh-*-allinone.jar app.jar

ENV SPARK_LOCAL_IP=127.0.0.1
ENV SPARK_DRIVER_HOST=127.0.0.1

ENTRYPOINT ["java", "-jar", "app.jar"]