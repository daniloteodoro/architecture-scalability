#!/usr/bin/env bash

# Set your own user on docker hub in the .env file (DOCKERHUB_USER variable)
source ../../deployment/.env

if [[ -z $DOCKERHUB_USER ]]; then
  echo "Environment variable DOCKERHUB_USER is obligatory"
  exit 1
fi

cd "$(dirname "$0")" || exit 1
mkdir -p generated

echo "All pre-requisites set, replacing DOCKERHUB_USER inside yml files with: $DOCKERHUB_USER"

# Replace $DOCKERHUB_USER content with environment variable's content
for f in $(find ./queue ./elk ./core -regex '.*\.yml'); do
  envsubst < $f > "./generated/$(basename $f)";
done

kubectl apply -f ./generated/
