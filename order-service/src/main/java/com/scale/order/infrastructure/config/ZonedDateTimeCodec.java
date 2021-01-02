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

    public static final String EPOCH_FIELD = "epoch_milli";
    public static final String ZONE_FIELD = "zone";

    @Override
    public void encode(final BsonWriter writer, final ZonedDateTime value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeDateTime(EPOCH_FIELD, value.toInstant().toEpochMilli());
        writer.writeString(ZONE_FIELD, value.getZone().getId());
        writer.writeEndDocument();
    }

    @Override
    public ZonedDateTime decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartDocument();
        long epochMilli = reader.readDateTime(EPOCH_FIELD);
        String zoneId = reader.readString(ZONE_FIELD);
        reader.readEndDocument();

        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of(zoneId));
    }

    @Override
    public Class<ZonedDateTime> getEncoderClass() {
        return ZonedDateTime.class;
    }
}
