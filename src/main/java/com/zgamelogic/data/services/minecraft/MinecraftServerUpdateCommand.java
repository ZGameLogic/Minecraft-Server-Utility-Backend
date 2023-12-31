package com.zgamelogic.data.services.minecraft;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MinecraftServerUpdateCommand {
    private String server;
    private String category;
    private String version;
}
