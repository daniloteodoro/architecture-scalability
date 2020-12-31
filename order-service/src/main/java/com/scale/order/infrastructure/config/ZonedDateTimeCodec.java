package com.scale.order.infrastructure.config;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ZonedDateTimeCodec implements Codec<ZonedDateTime> {

    public static final String DATE_TIME = "dateTime";
    public static final String ZONE = "zone";

    @Override
    public void encode(final BsonWriter writer, final ZonedDateTime value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeDateTime(DATE_TIME, value.toInstant().getEpochSecond() * 1_000);
        writer.writeString(ZONE, value.getZone().getId());
        writer.writeEndDocument();
    }

    @Override
    public ZonedDateTime decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartDocument();
        long epochSecond = reader.readDateTime(DATE_TIME);
        String zoneId = reader.readString(ZONE);
        reader.readEndDocument();

        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond / 1_000), ZoneId.of(zoneId));
    }

    @Override
    public Class<ZonedDateTime> getEncoderClass() {
        return ZonedDateTime.class;
    }
}
