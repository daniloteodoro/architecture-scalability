package com.scale.check_out.domain.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public interface BusinessMetrics {

    /**
     * Validate session and reset the counter if necessary. Increase the counter of pending shopping carts.
     * @param sessionId
     * @param numberOfCustomers
     */
    void newShoppingCartStarted(String sessionId, Long numberOfCustomers);

    /**
     * Insert an entry containing timeToProcessInMs.
     * @param timeToProcessInMs
     */
    void finishShoppingCart(Long timeToProcessInMs, Long waitingTimeInMs, boolean isFirst, boolean isLast);

    /**
     * There is an ongoing timer that every second creates a metric summary containing amount of orders finished and
     * average time to process. Amount of orders finished also means Transactions per Second.
     * @return
     */
    ShoppingCartFinishedMetric[] pullAllFinishedShoppingCarts();

    // https://www.elastic.co/guide/en/beats/metricbeat/current/metricbeat-metricset-http-json.html
    @JsonInclude(JsonInclude.Include.NON_NULL)
    class ShoppingCartFinishedMetric {
        private final String sessionId;
        private final long numberOfCustomers;
        private final Long shoppingCartFinished;
        private final Long serviceTimeInMs;
        private final Long transactionsPerSecond;
        private final Long waitingTimeInMs;
        private final ZonedDateTime time;
        private final ZonedDateTime timeFirstFinished;
        private final ZonedDateTime timeLastFinished;

        private ShoppingCartFinishedMetric(String sessionId, Long numberOfCustomers, Long serviceTimeInMs, Long shoppingCartFinished, Long transactionsPerSecond,
                                          Long waitingTimeInMs, ZonedDateTime time, ZonedDateTime timeFirstFinished, ZonedDateTime timeLastFinished) {
            Objects.requireNonNull(sessionId, "Session id is mandatory");
            Objects.requireNonNull(numberOfCustomers, "Number of customers is mandatory");
            Objects.requireNonNull(time, "Time is mandatory");

            this.sessionId = sessionId;
            this.numberOfCustomers = numberOfCustomers;
            this.shoppingCartFinished = shoppingCartFinished;
            this.serviceTimeInMs = serviceTimeInMs;
            this.transactionsPerSecond = transactionsPerSecond;
            this.waitingTimeInMs = waitingTimeInMs;
            this.time = time;
            this.timeFirstFinished = timeFirstFinished;
            this.timeLastFinished = timeLastFinished;
        }

        public static ShoppingCartFinishedMetric withoutEvents(String sessionId, Long numberOfCustomers, ZonedDateTime time) {
            return new ShoppingCartFinishedMetric(sessionId, numberOfCustomers, null, null, null, null, time,
                    null, null);
        }

        public static ShoppingCartFinishedMetric withNoShoppingCartFinished(String sessionId, Long numberOfCustomers, ZonedDateTime time) {
            return new ShoppingCartFinishedMetric(sessionId, numberOfCustomers, null, 0L, 0L, null, time,
                    null, null);
        }

        public static ShoppingCartFinishedMetric withShoppingCartFinished(String sessionId, Long numberOfCustomers, Long serviceTimeInMs, Long shoppingCartFinished,
                                                                          Long transactionsPerSecond, Long waitingTimeInMs, ZonedDateTime time,
                                                                          ZonedDateTime timeFirstFinished, ZonedDateTime timeLastFinished) {
            Objects.requireNonNull(serviceTimeInMs, "ServiceTimeInMs is mandatory");
            Objects.requireNonNull(shoppingCartFinished, "ShoppingCartFinished is mandatory");
            Objects.requireNonNull(transactionsPerSecond, "TransactionsPerSecond is mandatory");
            Objects.requireNonNull(waitingTimeInMs, "WaitingTimeInMs is mandatory");
            return new ShoppingCartFinishedMetric(sessionId, numberOfCustomers, serviceTimeInMs, shoppingCartFinished, transactionsPerSecond, waitingTimeInMs, time,
                    timeFirstFinished, timeLastFinished);
        }

        public boolean happenedBeforeOrAtTheSameTimeAs(ZonedDateTime anotherTime) {
            return !this.time.isAfter(anotherTime);
        }

        @JsonProperty("session_id")
        public String getSessionId() {
            return this.sessionId;
        }
        @JsonProperty("number_of_customers")
        public Long getNumberOfCustomers() {
            return this.numberOfCustomers;
        }
        @JsonProperty("finished")
        public Long getShoppingCartFinished() {
            return shoppingCartFinished;
        }
        @JsonProperty("service_time_ms")
        public Long getServiceTimeInMs() {
            return this.serviceTimeInMs;
        }
        @JsonProperty("transactions_per_second")
        public Long getTransactionsPerSecond() {
            return transactionsPerSecond;
        }
        @JsonProperty("waiting_time_ms")
        public Long getWaitingTimeInMs() {
            return this.waitingTimeInMs;
        }
        @JsonProperty("time")
        public String getTime() { return this.time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME); }
        @JsonProperty("time_first_finished")
        public String getTimeFirstFinished() { return this.timeFirstFinished != null ? this.timeFirstFinished.format(DateTimeFormatter.ISO_ZONED_DATE_TIME) : null; }
        @JsonProperty("time_last_finished")
        public String getTimeLastFinished() { return this.timeLastFinished != null ? this.timeLastFinished.format(DateTimeFormatter.ISO_ZONED_DATE_TIME) : null; }
    }

}
