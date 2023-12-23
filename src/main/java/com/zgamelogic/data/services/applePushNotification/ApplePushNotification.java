package com.zgamelogic.data.services.applePushNotification;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.zgamelogic.data.serializers.applePushNotification.ApplePushNotificationSerializer;
import lombok.Data;

@Data
@JsonSerialize(using = ApplePushNotificationSerializer.class)
public class ApplePushNotification {
    private String title;
    private String subtitle;
    private String body;
}
