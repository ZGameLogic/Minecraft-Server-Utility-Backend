package com.zgamelogic.data.services.applePushNotification;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.zgamelogic.data.serializers.applePushNotification.ApplePushNotificationSerializer;
import lombok.Data;

@Data
@JsonSerialize(using = ApplePushNotificationSerializer.class)
public class ApplePushNotification {
    private final String title;
    private final String subtitle;
    private final String body;

    public ApplePushNotification(String title){
        this(title, null);
    }

    public ApplePushNotification(String title, String subtitle){
        this(title, subtitle, null);
    }
    public ApplePushNotification(String title, String subtitle, String body){
        this.title = title;
        this.subtitle = subtitle;
        this.body = body;
    }
}