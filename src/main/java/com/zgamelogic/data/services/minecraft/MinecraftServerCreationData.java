package com.zgamelogic.data.services.minecraft;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MinecraftServerCreationData {
    private boolean autoStart;
    private boolean autoUpdate;
    private String version;
    private String category;
    private String startCommand;
    private String updateScript;

    private int port;
    private String name;
}
