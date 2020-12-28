ARG ELK_VERSION

# https://www.docker.elastic.co/
FROM docker.elastic.co/logstash/logstash:${ELK_VERSION}

USER root

# Copy config files and pipelines
COPY ./config/logstash.yml /usr/share/logstash/config/logstash.yml
RUN chown logstash:logstash /usr/share/logstash/config/logstash.yml

COPY ./pipeline/logstash.conf /usr/share/logstash/pipeline/logstash.conf
RUN chown logstash:logstash /usr/share/logstash/pipeline/logstash.conf

USER logstash

# Add your logstash plugins setup here
# Example: RUN logstash-plugin install logstash-filter-json
