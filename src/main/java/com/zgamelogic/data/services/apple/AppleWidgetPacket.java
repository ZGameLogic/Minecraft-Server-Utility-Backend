package com.zgamelogic.data.services.apple;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.zgamelogic.data.serializers.apple.AppleWidgetPacketSerializer;
import com.zgamelogic.data.services.minecraft.MinecraftServer;
import lombok.Data;

import java.util.Collection;

@Data
@JsonSerialize(using = AppleWidgetPacketSerializer.class)
public class AppleWidgetPacket {
    private final Collection<MinecraftServer> minecraftServers;
}
