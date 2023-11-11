package com.zgamelogic.data.services.minecraft;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MinecraftServerConfig {
    private boolean autoStart;
    private boolean autoUpdate;
    private String version;
    private String startCommand;
    private String updateScript;
}
