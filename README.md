# architecture_scalability

Main purpose of this project is to show, in numbers (metrics), how the architectural choice and deployment affect performance and scalability.
Blocking and non-blocking architectures, REST, gRPC, and Reactive implementations, in search for higher throughput with low latency.

### Building and running as a cluster in Kubernetes (aws or minikube)
Enter directory `/architecture_scalability/k8/cluster` (I'll assume you're running scripts from there) <br>

Start by running either: <br>
* `./architecture_scalability/k8/cluster$ ./0-start-minikube.sh` (requires [minikube](https://minikube.sigs.k8s.io/docs/start/)) or <br>
* `./architecture_scalability/k8/cluster$ ./0-start-aws.sh <your_cluster_name>` (requires [kops](https://kops.sigs.k8s.io/getting_started/install/) installed).

Pass your cluster name to the AWS script. This option also requires a hosted zone configured in Route53 in AWS, which will default to your cluster name.
Kops will use AWS credentials from environment variables. Follow instructions given by kops after running the script.

Make sure the command `docker login` is working, so you can push images to your own repository.
Your repository name should be configured in the env file in `../../deployment/.env`. 
After that run the script to build and push ELK (ElasticSearch/Logstash/Kibana) images to your repository:

* `./architecture_scalability/k8/cluster$ ../../elk/elk-build-and-push.sh`

You will need the container images in your repo. We use [Jib](https://github.com/GoogleContainerTools/jib) for that. 
The pre-requisite to use jib is to have an environment variable DOCKERHUB_USER set to your docker hub repository.
You can source the .env file you previously configured for that, though the easiest way is to run the script on the deployment directory:

* `./architecture_scalability/k8/cluster$ ../../deployment/build_project_push_images.sh`

You can now deploy RabbitMQ, ElasticSearch/Logstash/Kibana and the services by running: <br>
* `./architecture_scalability/k8/cluster$ ./1-deploy-queue-elk-services.sh` <br>

**Important**: the script above replaces $DOCKERHUB_USER inside the Kubernetes deployment files!

Run `2-deploy-ingress-controller-minikube.sh` (Minikube) or `2-deploy-ingress-controller-nginx.sh` (for AWS) and finally `3-deploy-ingress.sh` to finish up the deployment.
It might take from a few to multiple minutes until all services become available, especially when deploying to AWS. <br>

Check section below "Monitoring performance" to see how you can play with it.

### Monitoring services with Kubernetes
Some handy commands to check how deployment is going: <br>
 * `kubectl get deployments`
* `kubectl get pods`
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

After around 1 minute the services should be ready - just be aware that fist run might take much longer, as Docker has to pull all images for rabbit and ELK. 
Check section below "Monitoring performance" to see how you can play with it.

### Monitoring performance
Here I'm using localhost (running locally), but depending on deployment type it can be the ip returned by the command `minikube ip` or `kubectl get ingress`, in case of the aws deployment.

Browse to `http://localhost:5601/` and log into kibana (default user `elastic`, password `changeme`).
There you will find the Dashboard named `Performance`. This dashboard uses a parameter `session_id` that will be explained shortly.

At this point you can create sample shopping carts using the Management api. In the terminal you could run: <br>
`curl -X POST http://localhost:9000/shopping-cart/samples/2000` <br>
or <br>
`curl -X POST http://localhost:9000/shopping-cart/samples/1000/for/30/seconds`

Both requests return a `session_id` as result, which you can copy and paste to Kibana's dashboard "Performance".
Remember to adjust the date/time ranges inside the Dashboard.

### Main management interface
The system is governed by the Management service. It contains the following endpoints:
* `/shopping-cart/samples/<number>`: generate a `<number>` of sample shopping carts and insert them into the queue to be processed.
* `/shopping-cart/samples/<number>/for/<duration>/seconds`: generate a `<number>` of sample shopping carts for `<duration>` seconds and keep inserting them into the queue to be processed.
For example: <br>
    `http://localhost:9000/shopping-cart/samples/1000/for/30/seconds` (local) or <br>
    `curl -X POST http://$(minikube ip)/shopping-cart/samples/10` (minikube) or <br>
    `curl -X POST http://$(kubectl get ingress -o=jsonpath='{.items[0].status.loadBalancer.ingress[0].hostname}')/shopping-cart/samples/10` (minikube)

#### Using your own domain
Inside Route53, select your Hosted Zone and copy its name servers (NS) to your custom domain (you can register one for free here http://www.dot.tk/). 
After that, create a new Record and fill in the section "Value/Route traffic to" as instructed below (at least when using the Create Record Wizard):
* Alias to Network Load Balancer
* < your aws region >
* < Associate with the ELB created previously >

You now should be able to reach your deployment using your custom domain, and the ingress controller will continue redirecting to services as usual:

Example using the test domain _scale-order.tk_ with the new record called "apps" <br>
* Kibana: http://apps.scale-order.tk/
* Management API: http://apps.scale-order.tk/shopping-cart/samples/10

// TODO: Add dashboard picture. Show how to switch between architecture types.
