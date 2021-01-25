# architecture_scalability

Main purpose of this project is to show in numbers how the architectural choice and deployment affect performance and scalability.
REST and gRPC, with synchronous and reactive implementations (async, non-blocking), in search for higher throughput and low latency.

### The idea
I wanted to see metrics like throughput, latency, and CPU usage, in various loads, with different architecture and deployment setups. 

![Dashboard in Kibana](https://github.com/daniloteodoro/architecture_scalability/blob/main/docs/scalability_dashboard_730tps.png?raw=true)

Latency, for example, affects contract SLAs and user experience (UX), and its analysis may help defining the maximum throughput allowed for an individual application).
Certain parameters like throughput can be estimated using [Little's law](https://en.wikipedia.org/wiki/Little%27s_law), but I preferred an empirical test.
As described below, by changing the shopping carts per second (arrival rate), or by adding/removing nodes in the cluster (capacity, scaling in/out), one can 
observe how the system with a certain architecture behaves, with live metrics. The metrics include the whole process from a user (request) point-of-view, e.g. latency starts counting 
when a fictional shopping cart is created and finishes when the respective order is completed.

![Steps until an order gets created](https://github.com/daniloteodoro/architecture_scalability/blob/main/docs/ProcessOrder-sequence-diagram.png?raw=true)

![Deployment diagram](https://github.com/daniloteodoro/architecture_scalability/blob/main/docs/container-diagram.png?raw=true)

### Building and running as a cluster in Kubernetes (minikube or AWS)
Enter directory `/architecture_scalability/k8/cluster` (I'll assume you're running scripts from there) <br>

Start by running either: <br>
* `./architecture_scalability/k8/cluster$ ./0-start-minikube.sh` (requires [minikube](https://minikube.sigs.k8s.io/docs/start/) installed) or <br>
* `./architecture_scalability/k8/cluster$ ./0-start-aws.sh <your_cluster_name>` (requires [kops](https://kops.sigs.k8s.io/getting_started/install/) installed).

Pass your cluster name to the AWS script. This option also requires a hosted zone configured in Route53 in AWS, which will default to your cluster name.
Kops will use AWS credentials from environment variables. Follow instructions given by kops after running the script.

**Important**: don't forget to delete your resources on AWS after use! (`kops delete cluster <your_cluster_name> --yes`)

Make sure the command `docker login` is working, so you can push images to your own repository.
Your repository name should be configured in the env file in `../../deployment/.env`. 
After that run the script to build and push ELK (ElasticSearch/Logstash/Kibana) images to your repository:

* `./architecture_scalability/k8/cluster$ ../../elk/elk-build-and-push.sh`

You will need the container images in your repo. We use [Jib](https://github.com/GoogleContainerTools/jib) for that. 
The pre-requisite to use jib is to have an environment variable DOCKERHUB_USER set to your docker hub repository.
You can source the .env file you previously configured for that, though the easiest way is to run the script on the deployment directory (you need maven installed):

* `./architecture_scalability/k8/cluster$ ../../deployment/build_project_push_images.sh`

You can now deploy RabbitMQ, ElasticSearch/Logstash/Kibana and the services by running: <br>
* `./architecture_scalability/k8/cluster$ ./1-deploy-storage-elk-services.sh` <br>

**Important**: the script above replaces $DOCKERHUB_USER inside the Kubernetes deployment files!

Run `2-deploy-ingress-controller-minikube.sh` (Minikube) or `2-deploy-ingress-controller-nginx.sh` (for AWS) and finally `3-deploy-ingress.sh` to finish up the deployment.
It might take from a few to multiple minutes until all services become available, especially when deploying to AWS. <br>

Check section below "Monitoring performance" to see how you can play with it.

### Monitoring services with Kubernetes
Some handy commands to check how deployment is going: <br>
 * `kubectl get deployments`
* `kubectl get pods`
* `kubectl logs <pod_name>`
* `kubectl describe pod <NAME>`
* `kubectl get ingress`
* `kubectl get service -n ingress-nginx`

### Building and running locally

Install [Metricbeat](https://www.elastic.co/guide/en/beats/metricbeat/6.8/metricbeat-installation.html) <br>
Enable metricbeat's output to logstash. You can take a look at the configuration I used looking into `/metricbeat/metricbeat.yml`
Enable the http module and log and configure it like in `/metricbeat/http.yml`

Build the projects <br>
./architecture_scalability$ `mvn clean install`

Then start the docker files inside directories `checkout-job/docker` and `elk`, and run each service, although the easier way is using the convenience script below: <br>
`./architecture_scalability$ ./deployment/local_deploy.sh`

After around 1 minute the services should be ready - just be aware that fist run will take much longer, as Docker has to pull all images for RabbitMq, MongoDb, ELK, etc. 
Check section below "Monitoring performance" to see how you can play with it.

### Monitoring performance
The process starts with the creation of shopping carts to be processed. These shopping carts will be processed by the 
checkout-job, which in turn uses the order service and payment service to transform the shopping cart in an actual completed order.
This process can be followed through metrics visualized inside Kibana.

1. Enter Kibana by pointing your browser to: `http://<ip>:5601/` (default user `elastic`, password `changeme`)
2. Find a dashboard named **Performance**. This dashboard uses a parameter `session_id` that will be explained shortly.
3. Generate some shopping carts by POSTing to `http://<ip>:9000/shopping-cart/samples/200`

The `<ip>` depends on your deployment type: <br>
**Local**: `127.0.0.1` <br>
**Minikube**: the ip returned by the command: `minikube ip` <br>
**AWS**: the ip returned by the command: `kubectl get ingress`

The POST request returns a `session_id`, which you can copy and paste into Kibana's dashboard "Performance".
Remember to adjust the date/time ranges inside the Dashboard.
![Dashboard with session id in Kibana](https://github.com/daniloteodoro/architecture_scalability/blob/main/docs/kibana_dashboard_sessionid.png?raw=true)

### Main management interface
The system is governed by the Management service. It contains the following endpoints:
* `/shopping-cart/samples/<number>`: generate a `<number>` of sample shopping carts and insert them into the queue to be processed.
* `/shopping-cart/samples/<number>/for/<duration>/seconds`: generate a `<number>` of sample shopping carts for `<duration>` seconds and keep inserting them into the queue to be processed.
For example: <br>
    `http://localhost:9000/shopping-cart/samples/1000/for/30/seconds` (local) or <br>
    `curl -X POST http://$(minikube ip)/shopping-cart/samples/10` (minikube) or <br>
    `curl -X POST http://$(kubectl get ingress -o=jsonpath='{.items[0].status.loadBalancer.ingress[0].hostname}')/shopping-cart/samples/10` (AWS)

#### Using your own domain
Inside Route53, select your Hosted Zone and copy its name servers (NS) to your custom domain (you can register one for free on http://www.dot.tk/). 
After that, create a new Record and fill in the section "Value/Route traffic to" as instructed below (at least when using the Create Record Wizard):
* Alias to Network Load Balancer
* < your AWS region >
* < Associate with the ELB created previously >

You now should be able to reach your deployment using your custom domain, and the ingress controller will continue redirecting to services as usual:

Example using the test domain _scale-order.tk_ with the new record called "apps" <br>
* Kibana: http://apps.scale-order.tk/
* Management API: http://apps.scale-order.tk/shopping-cart/samples/10
