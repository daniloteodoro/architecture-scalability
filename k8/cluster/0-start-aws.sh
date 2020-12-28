#!/usr/bin/env bash

# $1 = cluster name

if [[ -z $1 ]]; then
    echo "1st parameter - Cluster name - is obligatory"
    exit 1
fi

CLUSTERNAME=$1

echo "Creating cluster on AWS with name $CLUSTERNAME on zone eu-central-1 (1 node t2.large)"

# Optionally loads env vars from this file (I had 2 AWS accounts on my setup)
source ../../../aws_credentials.env

kops create cluster "$CLUSTERNAME" --zones=eu-central-1a --node-count=1 --node-size=t2.large --master-size=t3.medium --dns-zone="$CLUSTERNAME"
