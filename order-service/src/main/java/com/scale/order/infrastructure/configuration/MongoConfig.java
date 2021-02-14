package com.scale.order.infrastructure.configuration;

import com.mongodb.ConnectionString;

public class MongoConfig {
    private static final String DEFAULT_CS = "mongodb://orderservice:s89fsj&2#@127.0.0.1:27017/admin";

    public com.mongodb.client.MongoClient getBlockingClient() {
        // Get values from config file
        ConnectionString connectionString =
                new ConnectionString(System.getenv().getOrDefault("MONGOORDER_CS", DEFAULT_CS));

        return com.mongodb.client.MongoClients.create(connectionString);
    }

    public com.mongodb.reactivestreams.client.MongoClient getNonBlockingClient() {
        // Get values from config file
        ConnectionString connectionString =
                new ConnectionString(System.getenv().getOrDefault("MONGOORDER_CS", DEFAULT_CS));

        return com.mongodb.reactivestreams.client.MongoClients.create(connectionString);
    }

}
