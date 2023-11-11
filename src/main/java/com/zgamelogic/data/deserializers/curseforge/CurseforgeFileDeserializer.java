package com.zgamelogic.data.deserializers.curseforge;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.zgamelogic.data.services.curseforge.CurseforgeFile;

import java.io.IOException;

public class CurseforgeFileDeserializer extends JsonDeserializer<CurseforgeFile> {
    @Override
    public CurseforgeFile deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        CurseforgeFile file = new CurseforgeFile();
        JsonNode json = jsonParser.getCodec().readTree(jsonParser);
        JsonNode node = json.get("data");
        file.setDisplayName(node.get("displayName").asText());
        file.setDownloadUrl(node.get("downloadUrl").asText());
        return file;
    }
}
