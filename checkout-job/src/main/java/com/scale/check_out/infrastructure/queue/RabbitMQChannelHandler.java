package com.scale.check_out.infrastructure.queue;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.scale.check_out.application.services.CheckOutError;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

import java.util.Collections;

@Slf4j
public class RabbitMQChannelHandler {
    private final ConnectionFactory blockingConnectionFactory;
    private final ConnectionFactory nonBlockingConnectionFactory;

    public RabbitMQChannelHandler(String connectionString) {
        this.blockingConnectionFactory = getConnectionFactory(connectionString, false);
        this.nonBlockingConnectionFactory = getConnectionFactory(connectionString, true);
    }

    private Connection getConnection(ConnectionFactory factory) {
        try {
            return factory.newConnection();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CheckOutError("Failure setting up the connection to RabbitMQ");
        }
    }

    @NotNull
    private ConnectionFactory getConnectionFactory(String connectionString, Boolean nonBlocking) {
        try {
            ConnectionFactory result = new ConnectionFactory();
            result.setUri(connectionString);
            if (nonBlocking)
                result.useNio();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CheckOutError("Failure setting up the connection factory for RabbitMQ");
        }
    }

    public Channel createChannel() {
        try {
            log.info("Using queue url: {}:{} (blocking)", blockingConnectionFactory.getHost(), blockingConnectionFactory.getPort());
            var channel = getConnection(blockingConnectionFactory).createChannel();
            channel.basicQos(10, true);
            return channel;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CheckOutError("Failure configuring the queue channel");
        }
    }

    @SneakyThrows
    public AMQP.Queue.DeclareOk declareQueueBlocking(String queueName) {
        return getConnection(blockingConnectionFactory).createChannel()
                .queueDeclare(queueName, true, false, false, Collections.emptyMap());
    }

    public Mono<AMQP.Queue.DeclareOk> declareQueueNonBlocking(String queueName) {
        return createNonBlockingSender().declareQueue(QueueSpecification.queue(queueName)
                .durable(true)
                .exclusive(false)
                .autoDelete(false));
    }

    public Sender createNonBlockingSender() {
        // TODO: Close the sender
        SenderOptions senderOptions =  new SenderOptions()
                .connectionFactory(nonBlockingConnectionFactory)
                .resourceManagementScheduler(Schedulers.boundedElastic());

        return RabbitFlux.createSender(senderOptions);
    }

    public Receiver createNonBlockingReceiver() {
        // TODO: Close the receiver
        log.info("Using queue url: {}:{} (non-blocking)", nonBlockingConnectionFactory.getHost(), nonBlockingConnectionFactory.getPort());
        ReceiverOptions receiverOptions =  new ReceiverOptions()
                .connectionFactory(nonBlockingConnectionFactory)
                .connectionSubscriptionScheduler(Schedulers.boundedElastic());

        return RabbitFlux.createReceiver(receiverOptions);
    }

    public static void main(String[] args) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        try {
            connectionFactory.setUri("amqp://guest:guest@localhost");
            connectionFactory.useNio();

            ReceiverOptions receiverOptions =  new ReceiverOptions()
                    .connectionFactory(connectionFactory)
                    .connectionSubscriptionScheduler(Schedulers.boundedElastic());

            RabbitFlux.createReceiver(receiverOptions)
                    .consumeAutoAck("abc")
                    .subscribe(m -> {
                        System.out.println("Received message {}" + new String(m.getBody()));
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
