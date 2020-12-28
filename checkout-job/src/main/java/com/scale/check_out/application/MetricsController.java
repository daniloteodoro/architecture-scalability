package com.scale.check_out.application;

import com.scale.check_out.domain.metrics.BusinessMetrics;
import io.javalin.http.Context;

import java.util.Objects;
import java.util.logging.Logger;

public class MetricsController {
    private final BusinessMetrics metrics;
    private static final Logger logger = Logger.getLogger(MetricsController.class.getName());

    public MetricsController(BusinessMetrics metrics) {
        this.metrics = Objects.requireNonNull(metrics, "Business metrics object is required");
    }

    public void handleMetrics(Context context) {
        context.json(metrics.pullAllFinishedShoppingCarts());
        logger.info("Metrics were published");
    }

}
