package com.scale.check_out.infrastructure.metrics;

import com.scale.check_out.domain.metrics.SessionWasNotStartedError;
import com.scale.check_out.domain.metrics.BusinessMetrics;
import lombok.Value;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class BusinessMetricsInMemory implements BusinessMetrics {
    private final static Logger LOGGER = Logger.getLogger(BusinessMetricsInMemory.class.getName());
    private final static int TIME_OUT_IN_SECONDS = 30;
    private ZonedDateTime sessionStart = ZonedDateTime.now(ZoneOffset.UTC);
    private ZonedDateTime lastEventRead = ZonedDateTime.now(ZoneOffset.UTC);
    private String sessionId = null;
    private Long numberOfCustomers = 0L;
    private final AtomicInteger pendingShoppingCarts = new AtomicInteger(0);

    private final LinkedBlockingQueue<ShoppingCartFinishedMetric> metrics = new LinkedBlockingQueue<>();
    // TODO: Rethink this process
    private final LinkedBlockingQueue<PendingTransaction> pendingTransactions = new LinkedBlockingQueue<>();

    @Override
    public void newShoppingCartStarted(String sessionId, Long numberOfCustomers) {
        if (sessionId == null || sessionId.isBlank())
            throw new NullPointerException("Session cannot be empty");

        if (!sessionId.equals(this.sessionId)) {
            this.sessionId = sessionId;
            this.numberOfCustomers = numberOfCustomers;
            resetSession();
        }
        pendingTransactions.add(new PendingTransaction(ZonedDateTime.now(ZoneOffset.UTC), pendingShoppingCarts.incrementAndGet()));
    }

    private void resetSession() {
        this.sessionStart = ZonedDateTime.now(ZoneOffset.UTC);
        this.lastEventRead = sessionStart;
        metrics.clear();
        pendingTransactions.clear();
        pendingShoppingCarts.set(0);
        LOGGER.info("New session was started: " + sessionId);
    }

    // TODO: Object holding basic information can be simpler than the aggregate object sent to ELK.
    @Override
    public void finishShoppingCart(Long timeToProcessInMs, Long waitingTimeInMs, boolean isFirst, boolean isLast) {
        validateSession();
        var timeFirstFinished = isFirst ? ZonedDateTime.now(ZoneOffset.UTC) : null;
        var timeLastFinished = isLast ? ZonedDateTime.now(ZoneOffset.UTC) : null;
        metrics.add(ShoppingCartFinishedMetric.withShoppingCartFinished(this.sessionId, this.numberOfCustomers, timeToProcessInMs,
                1L, 1L, waitingTimeInMs, ZonedDateTime.now(ZoneOffset.UTC), timeFirstFinished, timeLastFinished));
        pendingShoppingCarts.decrementAndGet();
    }

    @Override
    public ShoppingCartFinishedMetric[] pullAllFinishedShoppingCarts() {
        if (!hasValidSession() || isExpired())
            return new ShoppingCartFinishedMetric[0];

        var result = new ArrayList<ShoppingCartFinishedMetric>();
        // Group availableMetrics per second - only interested in "closed" seconds, not partial values, which means
        // if this method is called and current time is < 1 second since session started, then an entry is not returned.
        // start time = 15:00:00.000
        //     item 1 = 15:00:00.025
        //     item 2 = 15:00:00.150
        //     item 3 = 15:00:00.540
        //     item 4 = 15:00:00.892_____________________________________Create an entry for this case => 15:00:00.892, 4 orders, TPS = 4, RT = 1000/4 = 250
        //     item 5 = 15:00:01.001
        //     .......

        // Start time is initially the <Session Start Time>, then after creating the first entry, Start time becomes the next item's start time, or current date/time.

        int itemsToRemove = 0;
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startTime = lastEventRead;
        ZonedDateTime endTime = lastEventRead;
        var steps = startTime.until(now, ChronoUnit.SECONDS);
        for (int step = 0; step < steps; step++) {
            endTime = startTime.plusSeconds(1);
            var measurements = new ArrayList<ProcessingTime>();
            while (metrics.peek() != null && metrics.peek().happenedBeforeOrAtTheSameTimeAs(endTime)) {
                var event = metrics.poll();
                if (event != null)
                    measurements.add(new ProcessingTime(event.getServiceTimeInMs(), event.getWaitingTimeInMs(), event.getTimeFirstFinished(), event.getTimeLastFinished()));
            }
            result.add(generateSummary(measurements, startTime, endTime));
            itemsToRemove += measurements.size();
            startTime = endTime;
        }

        this.lastEventRead = endTime;

        // Remove 'n' items from the auxiliary list "pendingTransactions"
        final ZonedDateTime lastRead = endTime;
        pendingTransactions.removeIf(transaction -> transaction.time.isBefore(lastRead));

        return result.toArray(new ShoppingCartFinishedMetric[0]);
    }

    private boolean hasPendingTransaction(ZonedDateTime from, ZonedDateTime to) {
        for (PendingTransaction current : pendingTransactions) {
            if (current.isBetween(from, to))
                return (current.pendingTransactions > 0);
        }

        return false;
    }

    private ShoppingCartFinishedMetric generateSummary(ArrayList<ProcessingTime> measurements, ZonedDateTime start, ZonedDateTime end) {
        if (measurements.isEmpty()) {
            if (hasPendingTransaction(start, end))
                return ShoppingCartFinishedMetric.withNoShoppingCartFinished(this.sessionId, this.numberOfCustomers, end);
            else
                return ShoppingCartFinishedMetric.withoutEvents(this.sessionId, this.numberOfCustomers, end);
        }

        long totalServiceTime = measurements.stream()
                .map(ProcessingTime::getServiceTimeInMs)
                .reduce(Long::sum)
                .orElse(0L);
        long totalWaitingTime = measurements.stream()
                .map(ProcessingTime::getWaitingTimeInMs)
                .reduce(Long::sum)
                .orElse(0L);
        ZonedDateTime timeFirstFinished = measurements.stream()
                .map(ProcessingTime::getTimeFirstFinished)
                .filter(Objects::nonNull)
                .findFirst()
                .map(ZonedDateTime::parse)
                .orElse(null);
        ZonedDateTime timeLastFinished = measurements.stream()
                .map(ProcessingTime::getTimeLastFinished)
                .filter(Objects::nonNull)
                .findFirst()
                .map(ZonedDateTime::parse)
                .orElse(null);

        long averageServiceTime = totalServiceTime / measurements.size();
        long averageWaitingTime = totalWaitingTime / measurements.size();
        long tps = measurements.size();

        return ShoppingCartFinishedMetric.withShoppingCartFinished(this.sessionId, this.numberOfCustomers, averageServiceTime, (long) measurements.size(), tps, averageWaitingTime,
                end, timeFirstFinished, timeLastFinished);
    }

    private void validateSession() {
        if (!hasValidSession())
            throw new SessionWasNotStartedError();
    }

    private boolean hasValidSession() {
        return (sessionId != null && !sessionId.isBlank());
    }

    private boolean isExpired() {
        return !hasValidSession() ||
                ChronoUnit.SECONDS.between(lastEventRead, ZonedDateTime.now(ZoneOffset.UTC)) > TIME_OUT_IN_SECONDS;
    }

    public static class ProcessingTime {
        private final Long serviceTimeInMs;
        private final Long waitingTimeInMs;
        private final String timeFirstFinished;
        private final String timeLastFinished;
        public ProcessingTime(Long serviceTimeInMs, Long waitingTimeInMs, String timeFirstFinished, String timeLastFinished) {
            this.serviceTimeInMs = serviceTimeInMs;
            this.waitingTimeInMs = waitingTimeInMs;
            this.timeFirstFinished = timeFirstFinished;
            this.timeLastFinished = timeLastFinished;
        }
        public Long getServiceTimeInMs() {
            return serviceTimeInMs;
        }
        public Long getWaitingTimeInMs() {
            return waitingTimeInMs;
        }
        public String getTimeFirstFinished() {
            return timeFirstFinished;
        }
        public String getTimeLastFinished() {
            return timeLastFinished;
        }
    }

    @Value
    static class PendingTransaction {
        ZonedDateTime time;
        Integer pendingTransactions;

        public boolean isBetween(ZonedDateTime from, ZonedDateTime to) {
            return (time.isAfter(from) && time.isBefore(to));
        }
    }

}
