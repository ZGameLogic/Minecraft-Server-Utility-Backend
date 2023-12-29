package com.zgamelogic.data.serializers.applePushNotification;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zgamelogic.data.services.applePushNotification.ApplePushNotification;

import java.io.IOException;

public class ApplePushNotificationSerializer extends JsonSerializer<ApplePushNotification> {
    @Override
    public void serialize(ApplePushNotification notification, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectFieldStart("aps");
        jsonGenerator.writeStringField("sound", "bingbong.aiff");
        jsonGenerator.writeObjectFieldStart("alert");
        jsonGenerator.writeStringField("title", notification.getTitle());

        if (notification.getSubtitle() != null) {
            jsonGenerator.writeStringField("subtitle", notification.getSubtitle());
        }

        if (notification.getBody() != null) {
            jsonGenerator.writeStringField("body", notification.getBody());
        }

        jsonGenerator.writeEndObject(); // alert
        jsonGenerator.writeEndObject(); // aps
        jsonGenerator.writeEndObject();
    }
}