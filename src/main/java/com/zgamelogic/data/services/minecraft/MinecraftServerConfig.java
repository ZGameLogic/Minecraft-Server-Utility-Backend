package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftServerConfig {
    private boolean autoStart;
    private boolean autoUpdate;
    private String version;
    private String category;
    private String startCommand;

    public MinecraftServerConfig(MinecraftServerCreationData data){
        autoStart = data.getAutoStart();
        autoUpdate = data.getAutoUpdate();
        version = data.getVersion();
        category = data.getCategory();
        startCommand = data.getStartCommand();
    }
}
