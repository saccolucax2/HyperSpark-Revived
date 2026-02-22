cd simulation-core
mvn clean package
cd ..
docker-compose down
docker-compose build --no-cache