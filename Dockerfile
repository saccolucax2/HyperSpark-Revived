# 1. Immagine Base
FROM eclipse-temurin:8-jre

# 2. Cartella di lavoro
WORKDIR /app

# 3. Copia il JAR "All-in-one"
COPY target/hyperh-*-allinone.jar app.jar

# 4. Variabili Spark
ENV SPARK_LOCAL_IP=127.0.0.1
ENV SPARK_DRIVER_HOST=127.0.0.1

# 5. Avvio
ENTRYPOINT ["java", "-jar", "app.jar"]