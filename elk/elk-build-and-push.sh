#!/usr/bin/env bash

cd "$(dirname "$0")" || exit 1

if ! docker login; then
  echo "Failure to login with docker, please test command 'docker login' first"
  exit 1
fi

# Set your own user on docker hub in the .env file (DOCKERHUB_USER variable)
source ../deployment/.env
# ELK version (currently 7.10.2)
source .env

if [[ -z $ELK_VERSION ]]; then
  echo "Environment variable ELK_VERSION is obligatory"
  exit 1
fi

if [[ -z $DOCKERHUB_USER ]]; then
  echo "Environment variable DOCKERHUB_USER is obligatory"
  exit 1
fi

LOCAL_VERSION="0.5"
echo "Using ELK version $ELK_VERSION and pushing images to $DOCKERHUB_USER with version $LOCAL_VERSION"

ELASTICSEARCH_TAGS="-t elasticsearch:$LOCAL_VERSION -t $DOCKERHUB_USER/elasticsearch:$LOCAL_VERSION -t $DOCKERHUB_USER/elasticsearch:latest"

echo "Building ElasticSearch image ..."
docker image build ${ELASTICSEARCH_TAGS} ./elasticsearch/ --build-arg ELK_VERSION=${ELK_VERSION} &&
  docker push "$DOCKERHUB_USER/elasticsearch"
echo "Done with ElasticSearch"

# Not needed for now
#LOGSTASH_TAGS="-t logstash:$LOCAL_VERSION -t $DOCKERHUB_USER/logstash:$LOCAL_VERSION -t $DOCKERHUB_USER/logstash:latest"
#
#echo "Building Logstash image ..."
#docker image build ${LOGSTASH_TAGS} ./logstash/ --build-arg ELK_VERSION=${ELK_VERSION} &&
#  docker push "$DOCKERHUB_USER/logstash"
#echo "Done with Logstash"

KIBANA_TAGS="-t kibana:$LOCAL_VERSION -t $DOCKERHUB_USER/kibana:$LOCAL_VERSION -t $DOCKERHUB_USER/kibana:latest"

echo "Building Kibana image ..."
docker image build ${KIBANA_TAGS} ./kibana/ --build-arg ELK_VERSION=${ELK_VERSION} &&
  docker push "$DOCKERHUB_USER/kibana"
echo "Done with Kibana"
