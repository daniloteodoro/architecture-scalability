package com.scale.management.application.controller;

import com.scale.management.application.ManagementError;
import com.scale.management.application.usecases.AddSampleShoppingCarts;
import io.javalin.http.Context;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import java.util.concurrent.Executors;

@AllArgsConstructor
@Slf4j
public class ShoppingCartController {

    private final @NonNull AddSampleShoppingCarts addSampleShoppingCarts;

    // POST /shopping-cart/samples/:number
    public void handleNewSamples(Context context) {
        var paramNumber = context.pathParam("number");
        if (paramNumber.isBlank())
            throw new ManagementError("Param 'number' is mandatory");

        var command = AddSampleShoppingCarts.AddShoppingCartCommand.builder()
                .number(Integer.parseInt(paramNumber))
                .maxNumberOfClients(Integer.parseInt(paramNumber))
                .ackFirst(true)
                .ackLast(true)
                .build();

        var session = addSampleShoppingCarts.handle(command);

        log.info(String.format("%s sample shopping carts were published", paramNumber));

        context.result(session)
                .status(HttpStatus.CREATED_201);
    }

    // POST /shopping-cart/samples/:number/for/:interval/seconds
    public void handleNewSamplesEveryXSeconds(Context context) {
        var paramNumber = context.pathParam("number");
        var paramInterval = context.pathParam("interval");
        if (paramNumber.isBlank())
            throw new ManagementError("Param 'number' is mandatory");
        if (paramInterval.isBlank())
            throw new ManagementError("Param 'interval' is mandatory");

        String sessionId = postSampleShoppingCartsForXSeconds(Integer.parseInt(paramNumber), Integer.parseInt(paramInterval));

        context.result(sessionId)
                .status(HttpStatus.CREATED_201);
    }

    private String postSampleShoppingCartsForXSeconds(int numberOfSamples, int seconds) {
        String sessionId = addSampleShoppingCarts.startNewSession();
        Executors.newSingleThreadExecutor().submit(() -> {
            log.info(String.format("Started posting %d sample shopping carts for %d seconds for session %s", numberOfSamples, seconds, sessionId));
            for (int i = 0; i < seconds; i++) {
                var start = System.currentTimeMillis();
                postSamplesToSession(numberOfSamples, sessionId, (i == 0), (i == (seconds-1)), (long) numberOfSamples * seconds);
                var remainingTime = 1000 - (System.currentTimeMillis() - start);
                if (remainingTime < 1) {
                    log.error("Time to post %d sample shopping carts exceed 1 second (unstable)");
                    break;
                }
                try {
                    Thread.sleep(remainingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            log.info(String.format("Finished posting sample shopping carts for session %s", sessionId));
        });
        return sessionId;
    }

    private void postSamplesToSession(int number, String sessionId, boolean ackFirst, boolean ackLast, long maxNumberOfClients) {
        var command = AddSampleShoppingCarts.AddShoppingCartCommand.builder()
                .number(number)
                .session(sessionId)
                .maxNumberOfClients(maxNumberOfClients)
                .ackFirst(ackFirst)
                .ackLast(ackLast)
                .build();

        addSampleShoppingCarts.handle(command);

        log.info(String.format("%d sample shopping carts were published", number));
    }

}
