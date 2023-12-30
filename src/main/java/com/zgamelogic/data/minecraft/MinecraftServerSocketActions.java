package com.zgamelogic.data.minecraft;

import lombok.Data;

@Data
public class MinecraftServerSocketActions {
    private final MinecraftServerSocketAction messageAction;
    private final MinecraftServerSocketAction statusAction;
    private final MinecraftServerSocketAction playerAction;
    private final MinecraftServerSocketAction updateAction;
}
