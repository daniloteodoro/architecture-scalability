#!/usr/bin/env bash

curl --retry 8 -s -L -o jq https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64 && chmod +x jq

kibanaIndexCount=$(curl -X GET "localhost:5601/api/saved_objects/_find?type=index-pattern&search_fields=title&search=logstash" \
-H 'kbn-xsrf: true' -H 'Content-Type: application/json' -u elastic:changeme -s | ./jq -r '.total')

if [[ ! "${kibanaIndexCount}" -eq 1 ]]; then
  #  Import index along with other objects, like dashboard
  echo "Logstash index not found, importing objects..."
  curl -X POST "localhost:5601/api/saved_objects/_import?overwrite=true" -H 'kbn-xsrf: true' \
    --form file="@/usr/share/kibana/index_and_dashboard.ndjson" -u elastic:changeme \
      && echo "Import completed successfully" \
      || echo "Failure importing objects"
fi
