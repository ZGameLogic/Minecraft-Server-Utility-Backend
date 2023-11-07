package com.zgamelogic.data.services.minecraft;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@NoArgsConstructor
@Data
public class MinecraftServer {
    private String name;
    private int port;

    public MinecraftServer(File serverDir){

    }
}
