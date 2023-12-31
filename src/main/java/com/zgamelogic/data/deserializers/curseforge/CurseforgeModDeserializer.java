package com.zgamelogic.data.deserializers.curseforge;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.zgamelogic.data.services.curseforge.CurseforgeFile;
import com.zgamelogic.data.services.curseforge.CurseforgeMod;
import com.zgamelogic.services.CurseforgeService;

import java.io.IOException;

public class CurseforgeModDeserializer extends JsonDeserializer<CurseforgeMod> {
    @Override
    public CurseforgeMod deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        CurseforgeMod mod = new CurseforgeMod();
        JsonNode json = jsonParser.getCodec().readTree(jsonParser);
        JsonNode node = json.get("data");

        mod.setName(node.get("name").asText());
        mod.setMainFileId(node.get("mainFileId").asLong());
        mod.setSummary(node.get("summary").asText());
        mod.setDownloadCount(node.get("downloadCount").asLong());
        mod.setLogoUrl(node.get("logo").get("url").asText());
        mod.setUrl(node.get("links").get("websiteUrl").asText());
        mod.setServerFileName("");
        mod.setServerFileUrl("");
        mod.setFileName("");

        node.get("latestFiles").forEach(file -> {
            if(file.get("id").asLong() != mod.getMainFileId()) return;
            try {
                mod.setFileName(file.get("displayName").asText());
                long serverPackFileId = file.get("serverPackFileId").asLong();
                CurseforgeFile serverFile = CurseforgeService.getCurseforgeFile(node.get("id").asLong(), serverPackFileId);
                mod.setServerFileName(serverFile.getDisplayName());
                mod.setServerFileUrl(serverFile.getDownloadUrl());
            } catch (Exception ignored){}
        });
        mod.setValid(true);
        return mod;
    }
}
