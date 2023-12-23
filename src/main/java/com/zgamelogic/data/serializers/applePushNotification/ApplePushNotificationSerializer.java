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
        jsonGenerator.writeStringField("Simulator Target Bundle", notification.getSimulatorTargetBundle());

        if (notification.getAps() != null && notification.getAps().getAlert() != null) {
            jsonGenerator.writeObjectFieldStart("aps");
            jsonGenerator.writeObjectFieldStart("alert");

            jsonGenerator.writeStringField("title", notification.getAps().getAlert().getTitle());

            if (notification.getAps().getAlert().getSubtitle() != null) {
                jsonGenerator.writeStringField("subtitle", notification.getAps().getAlert().getSubtitle());
            }

            if (notification.getAps().getAlert().getBody() != null) {
                jsonGenerator.writeStringField("body", notification.getAps().getAlert().getBody());
            }

            jsonGenerator.writeEndObject(); // alert
            jsonGenerator.writeEndObject(); // aps
        }

        jsonGenerator.writeEndObject();
    }
}
