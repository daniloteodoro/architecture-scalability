FROM adoptopenjdk/openjdk15:latest

USER root

RUN echo "Installing MetricBeat..."

RUN curl -L -O https://artifacts.elastic.co/downloads/beats/metricbeat/metricbeat-7.10.2-amd64.deb
RUN dpkg -i metricbeat-7.10.2-amd64.deb

RUN echo "Configuring MetricBeat..."

# Testing docker file with:
# docker image build -t test-metricbeat:0.1 .
# docker container run -it test-metricbeat:0.1 /bin/bash

RUN metricbeat modules enable http
# RUN metricbeat modules enable docker (kubernetes?)

COPY ./metricbeat.yml /etc/metricbeat/metricbeat.yml
RUN chmod 644 /etc/metricbeat/metricbeat.yml

COPY ./http.yml /etc/metricbeat/modules.d/http.yml
RUN chmod 644 /etc/metricbeat/modules.d/http.yml
COPY ./system.yml /etc/metricbeat/modules.d/system.yml
RUN chmod 644 /etc/metricbeat/modules.d/system.yml
#RUN chown metricbeat:metricbeat /etc/metricbeat/modules.d/http.yml

# Log directory
RUN mkdir /root/metricbeat
RUN chmod -R 775 /root/metricbeat

RUN echo "Done configuring metribeat"

ENTRYPOINT service metricbeat start && java -cp /app/resources:/app/classes:/app/libs/* com.scale.check_out.StartingPoint
