#!/usr/bin/env bash

# Run from the context of the script's directory
cd "$(dirname "$0")" || exit 1

sudo service metricbeat stop;

echo "Stopping services"
pgrep -f dependencies.jar | xargs kill

cd ../checkout-job/docker || exit 1
docker-compose down
cd - || exit 1
echo "Rabbit-mq is being stopped"

cd ../order-service/docker || exit 1
docker-compose down
cd - || exit 1

cd ../payment-service/docker || exit 1
docker-compose down
cd - || exit 1
echo "MongoDb instances are being stopped"

cd ../elk || exit 1
docker-compose down
cd - || exit 1
echo "ELK is being stopped"

rm order.log payment.log checkout.log management.log

echo "Done"
