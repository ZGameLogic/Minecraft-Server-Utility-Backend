package com.zgamelogic.data.deserializers.minecraft;


import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zgamelogic.data.services.minecraft.MinecraftServerPingData;

import java.io.IOException;
import java.util.LinkedList;

public class MinecraftServerPingDataDeserializer extends JsonDeserializer<MinecraftServerPingData> {
    @Override
    public MinecraftServerPingData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        MinecraftServerPingData data = new MinecraftServerPingData();
        ObjectMapper om = (ObjectMapper) jsonParser.getCodec();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        data.setOnlineCount(node.get("players").get("online").asInt());
        LinkedList<String> players = new LinkedList<>();
        if(node.get("players").has("sample")){
            node.get("players").get("sample").forEach(n -> players.add(n.get("name").asText()));
        }
        data.setPlayers(players);

        return data;
    }
}
