package com.scale.check_out.infrastructure.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;

import java.io.IOException;
import java.time.ZonedDateTime;

public class SerializerConfig {

    public static void initializeUniRestWithGson() {
        Unirest.config().setObjectMapper(new ObjectMapper() {
            private final Gson gson = buildSerializer();

            public <T> T readValue(String s, Class<T> aClass) {
                try{
                    return gson.fromJson(s, aClass);
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object o) {
                try{
                    return gson.toJson(o);
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static Gson buildSerializer() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .registerTypeAdapter(ZonedDateTime.class, new TypeAdapter<ZonedDateTime>() {
                    @Override
                    public void write(JsonWriter out, ZonedDateTime value) throws IOException {
                        out.value(value.toString());
                    }
                    @Override
                    public ZonedDateTime read(JsonReader in) throws IOException {
                        return ZonedDateTime.parse(in.nextString());
                    }
                })
                .enableComplexMapKeySerialization()
                .create();
    }

}
