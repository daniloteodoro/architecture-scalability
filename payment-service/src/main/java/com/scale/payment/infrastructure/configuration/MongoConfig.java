package com.scale.payment.infrastructure.configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

// TODO: This class can be shared among modules
public class MongoConfig {

    public MongoClient getClient() {
        // Get values from config file
        ConnectionString connectionString =
                new ConnectionString(System.getenv().getOrDefault("MONGOPAYMENT_CS", "mongodb://paymentservice:ps89fsj&2#@0.0.0.0:27018/admin"));

        // TODO: Remove automatic POJO mappers
        CodecRegistry pojoCodecRegistry = fromProviders(
                PojoCodecProvider.builder()
                        .automatic(true)
                        .build()
        );
        CodecRegistry codecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                pojoCodecRegistry);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .build();

        return MongoClients.create(clientSettings);
    }

    public MongoDatabase getDatabase() {
        return getClient().getDatabase("payment_db");
    }

}
