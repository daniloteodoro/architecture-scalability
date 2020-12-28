package com.scale.management.infrastructure.queue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.scale.management.domain.model.ManagementError;
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
            throw new ManagementError("Failure configuring the queue connection");
        }
    }

    public Channel createChannel() {
        String queueConnectionUrl = System.getenv().getOrDefault("AMQP_URL", "amqp://guest:guest@localhost");
        log.info("Using queue url: {}", queueConnectionUrl);
        try {
            return getConnection(queueConnectionUrl)
                    .createChannel();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ManagementError("Failure configuring the queue channel");
        }
    }

}
