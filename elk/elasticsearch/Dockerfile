ARG ELK_VERSION

# https://www.docker.elastic.co/
FROM docker.elastic.co/elasticsearch/elasticsearch:${ELK_VERSION}

USER root

# Copy config files
COPY ./config/elasticsearch.yml /usr/share/elasticsearch/config/elasticsearch.yml
RUN chown elasticsearch:elasticsearch /usr/share/elasticsearch/config/elasticsearch.yml

USER elasticsearch

# Add your elasticsearch plugins setup here
# Example: RUN elasticsearch-plugin install analysis-icu
