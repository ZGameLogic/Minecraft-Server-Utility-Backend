package com.zgamelogic.data.services.minecraft;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MinecraftServerConfig {
    private boolean autoStart;
    private boolean autoUpdate;
    private String version;
    private String category;
    private String startCommand;
    private String updateScript;

    public MinecraftServerConfig(MinecraftServerCreationData data){
        autoStart = data.getAutoStart();
        autoUpdate = data.getAutoUpdate();
        version = data.getVersion();
        category = data.getCategory();
        startCommand = data.getStartCommand();
        updateScript = data.getUpdateScript();
    }
}
