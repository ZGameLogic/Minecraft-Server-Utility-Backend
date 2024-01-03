package com.zgamelogic.data.serializers.apple;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zgamelogic.data.services.apple.AppleWidgetPacket;
import com.zgamelogic.data.services.minecraft.MinecraftServer;

import java.io.IOException;

import static com.zgamelogic.data.Constants.MC_SERVER_ONLINE;

public class AppleWidgetPacketSerializer extends JsonSerializer<AppleWidgetPacket> {
    @Override
    public void serialize(AppleWidgetPacket appleWidgetPacket, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        int totalServers = appleWidgetPacket.getMinecraftServers().size();
        long onlineServers = appleWidgetPacket.getMinecraftServers().stream().filter(
                minecraftServer -> minecraftServer.getStatus().equals(MC_SERVER_ONLINE)
        ).count();

        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        jsonGenerator.setPrettyPrinter(prettyPrinter);

        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("total_servers", totalServers);
        jsonGenerator.writeNumberField("online_servers", onlineServers);
        jsonGenerator.writeArrayFieldStart("servers");
        for(MinecraftServer server: appleWidgetPacket.getMinecraftServers()){
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("name", server.getName());
            jsonGenerator.writeStringField("status", server.getStatus());
            jsonGenerator.writeArrayFieldStart("players");
            for(String player: server.getOnline()){
                jsonGenerator.writeString(player);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
