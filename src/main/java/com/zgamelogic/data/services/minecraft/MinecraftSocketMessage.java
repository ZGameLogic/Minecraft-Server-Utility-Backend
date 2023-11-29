package com.zgamelogic.data.services.minecraft;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MinecraftSocketMessage {
    private String messageType;
    private Object message;
    private String server;
}
