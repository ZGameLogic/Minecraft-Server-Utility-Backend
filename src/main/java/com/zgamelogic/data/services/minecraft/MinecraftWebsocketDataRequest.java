package com.zgamelogic.data.services.minecraft;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;

@Getter
@ToString
@NoArgsConstructor
public class MinecraftWebsocketDataRequest {
    private String action;
    private String userId;
    private HashMap<String, String> data;
}
