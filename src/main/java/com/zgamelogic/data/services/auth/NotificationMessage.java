package com.zgamelogic.data.services.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

@Data
public class NotificationMessage {
    public enum Toggle {
        PLAYER,
        CHAT,
        LIVE,
        STATUS;

        @JsonCreator
        public static Toggle fromString(String value) {
            return valueOf(value.toUpperCase());
        }
    }

    private String server;
    private Toggle notification;
}