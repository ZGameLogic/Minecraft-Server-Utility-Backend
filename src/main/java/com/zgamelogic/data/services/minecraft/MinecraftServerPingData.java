package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zgamelogic.data.deserializers.minecraft.MinecraftServerPingDataDeserializer;
import lombok.*;

import java.util.LinkedList;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = MinecraftServerPingDataDeserializer.class)
public class MinecraftServerPingData {
    private int onlineCount;
    private LinkedList<String> players;
}
