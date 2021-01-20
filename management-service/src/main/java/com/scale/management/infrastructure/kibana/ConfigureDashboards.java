package com.scale.management.infrastructure.kibana;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import kong.unirest.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
public class ConfigureDashboards {

    public static void forKibana() {
        new Thread("configure-dashboard") {
            @Override
            public void run() {
                try {
                    final String kibanaHost = System.getenv().getOrDefault("KIBANA_HOST", "localhost");
                    final String configFilenameStr = System.getenv().getOrDefault("INDEX_CONFIG_FILE", "");
                    final InputStream configFile = configFilenameStr.isBlank() ? getLocalDashboardConfiguration() : new FileInputStream(configFilenameStr);

                    RetryConfig config = RetryConfig.custom()
                            .maxAttempts(10)
                            .waitDuration(Duration.ofMillis(5000))
                            .retryExceptions(Exception.class)
                            .build();
                    Retry retry = Retry.of("kibanaConfigRetry", config);
                    retry.executeRunnable(() -> applyTemplate(kibanaHost, configFile));
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }.start();
    }

    private static void applyTemplate(String kibanaHost, InputStream configFile) {
        log.info("Checking Kibana setup...");
        HttpResponse<JsonNode> response = Unirest.get(String.format("http://%s:5601/api/saved_objects/_find", kibanaHost))
                .queryString("type", "index-pattern")
                .queryString("search_fields", "title")
                .queryString("search", "logstash")
                .header("kbn-xsrf", "true")
                .header("Content-Type", "application/json")
                .basicAuth("elastic", "changeme")
                .asJson();
        if (response.getStatus() != 200) {
            log.warn("Kibana's not reachable yet.");
            throw new DashboardConfigurationError(String.format("Failure %d contacting Kibana on host '%s'", response.getStatus(), kibanaHost));
        }

        if (response.getBody().getObject().optInt("total", 0) == 0) {
            log.info("Importing index and dashboard to Kibana");
            response = Unirest.post(String.format("http://%s:5601/api/saved_objects/_import", kibanaHost))
                    .queryString("overwrite", "true")
                    .header("kbn-xsrf", "true")
                    .basicAuth("elastic", "changeme")   // ugh!
                    .field("file", configFile, ContentType.TEXT_PLAIN, "index_and_dashboard.ndjson")
                    .asJson();
            if (response.getStatus() != 200)
                throw new DashboardConfigurationError(String.format("Error %d importing configurations to Kibana on host '%s'", response.getStatus(), kibanaHost));

            log.info("Kibana is configured");
        } else {
            log.info("Kibana is already configured");
        }
    }

    private static InputStream getLocalDashboardConfiguration() throws FileNotFoundException {
        return ConfigureDashboards.class.getClassLoader().getResourceAsStream("setup/index_and_dashboard.ndjson");
    }

    static class DashboardConfigurationError extends RuntimeException {
        public DashboardConfigurationError(String msg) {
            super(msg);
        }
    }

}
