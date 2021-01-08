package com.scale.payment.infrastructure.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.ZonedDateTime;

// TODO: This class can be shared among modules
public class SerializerConfig {

    public static Gson buildSerializer() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .registerTypeAdapter(ZonedDateTime.class, new TypeAdapter<ZonedDateTime>() {
                    @Override
                    public void write(JsonWriter out, ZonedDateTime value) throws IOException {
                        if (value != null)
                            out.value(value.toString());
                        else
                            out.nullValue();
                    }
                    @Override
                    public ZonedDateTime read(JsonReader in) throws IOException {
                        var value = in.nextString();
                        if (value == null || value.isBlank())
                            return null;

                        return ZonedDateTime.parse(value);
                    }
                })
                .enableComplexMapKeySerialization()
                .create();
    }

}
