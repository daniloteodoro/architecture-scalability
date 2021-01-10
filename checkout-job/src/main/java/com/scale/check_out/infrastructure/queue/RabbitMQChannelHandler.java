package com.scale.check_out.infrastructure.queue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.scale.check_out.application.services.CheckOutError;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RabbitMQChannelHandler {
    private static Connection connection = null;

    private static Connection getConnection(String queueConnectionUrl) {
        if (connection != null)
            return connection;
        var factory = new ConnectionFactory();
        try {
            factory.setUri(queueConnectionUrl);
            connection = factory.newConnection();
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CheckOutError("Failure configuring the queue connection");
        }
    }

    public Channel createChannel() {
        String queueConnectionUrl = System.getenv().getOrDefault("AMQP_URL", "amqp://guest:guest@localhost");
        log.info("Using queue url: {}", queueConnectionUrl);
        try {
            var channel = getConnection(queueConnectionUrl)
                    .createChannel();
            channel.basicQos(10, true);
            return channel;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CheckOutError("Failure configuring the queue channel");
        }
    }

}
