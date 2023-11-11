package com.zgamelogic.data.services.curseforge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zgamelogic.data.deserializers.curseforge.CurseforgeFileDeserializer;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = CurseforgeFileDeserializer.class)
public class CurseforgeFile {
    private String displayName;
    private String downloadUrl;
}
