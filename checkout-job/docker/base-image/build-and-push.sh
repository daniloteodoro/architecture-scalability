#!/usr/bin/env bash

cd "$(dirname "$0")" || exit 1

# Set your own user on docker hub in the .env file (DOCKERHUB_USER variable)
if [[ -z $DOCKERHUB_USER ]]; then
  echo "Environment variable DOCKERHUB_USER is obligatory"
  exit 1
fi

echo "About to build the checkout-base-image"

docker image build -t checkout-base-image:0.4 . -t "$DOCKERHUB_USER/checkout-base-image:0.4" -t "$DOCKERHUB_USER/checkout-base-image:latest"
docker push "$DOCKERHUB_USER/checkout-base-image"

echo "Done"
