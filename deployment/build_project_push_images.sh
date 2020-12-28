#!/usr/bin/env bash

cd "$(dirname "$0")" || exit 1

if ! docker login; then
  echo "Failure to login with docker, please test command 'docker login' first"
  exit 1
fi

source .env
# Set your own user on docker hub in the .env file (DOCKERHUB_USER variable)
if [[ -z $DOCKERHUB_USER ]]; then
  echo "Environment variable DOCKERHUB_USER is obligatory"
  exit 1
fi

echo "Configuration ok, pushing images to $DOCKERHUB_USER"

if ! ../checkout-job/docker/base-image/build-and-push.sh; then
  echo "Failure to build checkout-base-image"
  exit 1
fi
echo "Base image was built"

# Go to root directory to build projects
cd .. || exit 1

# Build all modules in the project
mvn clean install

# Create and push docker images to repo
mvn -f ./checkout-job/pom.xml jib:build
mvn -f ./order-service/pom.xml jib:build
mvn -f ./management-service/pom.xml jib:build
