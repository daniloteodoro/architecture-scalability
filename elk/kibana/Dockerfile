ARG ELK_VERSION

# https://www.docker.elastic.co/
FROM docker.elastic.co/kibana/kibana:${ELK_VERSION}

USER root

# Copy config files
COPY ./config/kibana.yml /usr/share/kibana/config/kibana.yml
RUN chown kibana:kibana /usr/share/kibana/config/kibana.yml

COPY setup/index_and_dashboard.ndjson /usr/share/kibana/index_and_dashboard.ndjson
RUN chown kibana:kibana /usr/share/kibana/index_and_dashboard.ndjson

COPY setup/setup_index_and_dashboard.sh /usr/share/kibana/setup_index_and_dashboard.sh
RUN chown kibana:kibana /usr/share/kibana/setup_index_and_dashboard.sh \
    && chmod +x /usr/share/kibana/setup_index_and_dashboard.sh

USER kibana

# Add your kibana plugins setup here
# Example: RUN kibana-plugin install <name|url>
