#!/usr/bin/env bash

# Run from the context of the script's directory
cd "$(dirname "$0")" || exit 1

# start rabbit-mq
cd ../checkout-job/docker || exit 1
docker-compose down
docker-compose up --build -d
cd - || exit 1
echo "Rabbit-mq is getting started"

cd ../order-service/docker || exit 1
docker-compose down
docker-compose up --build -d
cd - || exit 1
echo "MongoDb for the Order service is getting started"

cd ../payment-service/docker || exit 1
docker-compose down
docker-compose up --build -d
cd - || exit 1
echo "MongoDb for the Payment service is getting started"

cd ../elk || exit 1
docker-compose down
docker-compose up --build -d
cd - || exit 1
echo "ELK is getting started"

if ! sudo service metricbeat start;
then
  echo "Failure to start metricbeat, check whether this service is installed and that you have the appropriate permissions"
  exit 1
fi
echo "Metricbeat service started. Waiting some time to start services..."

# Adjust waiting time according to your case. First time will take way longer as docker has to get all images.
sleep 30s

echo "Starting services"

nohup java -jar ../order-service/target/order-service-*-jar-with-dependencies.jar > order.log 2>&1 &
echo "Order service started"

nohup java -jar ../payment-service/target/payment-service-*-jar-with-dependencies.jar > payment.log 2>&1 &
echo "Payment service started"

nohup java -jar ../checkout-job/target/checkout-job-*-jar-with-dependencies.jar > checkout.log 2>&1 &
echo "Check-out job started"

nohup java -jar ../management-service/target/management-service-*-jar-with-dependencies.jar > management.log 2>&1 &
echo "Management service started"

tail -f order.log payment.log checkout.log management.log
