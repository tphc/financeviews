#!/bin/bash
./gradlew clean build
#docker build . -t financeviews-docker
#docker rm -f financeviews-container
#docker run --name financeviews-container financeviews-docker
docker-compose down
docker-compose build
docker-compose up