package com.scale.check_out.infrastructure.payment;

import com.scale.check_out.application.services.CheckOutError;
import com.scale.payment.PaymentServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentServiceChannelHandler {

    public PaymentServiceGrpc.PaymentServiceBlockingStub createBlockingStub() {
        try {
            String paymentApiHost = System.getenv().getOrDefault("PAYMENT_API_HOST", "127.0.0.1");
            log.info("Using payment-api host: {}", paymentApiHost);

            var channel = ManagedChannelBuilder.forAddress(paymentApiHost, 8100)
                    .usePlaintext()
                    .build();
            return PaymentServiceGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CheckOutError("Failure contacting the Payment service using gRPC.");
        }
    }

}
