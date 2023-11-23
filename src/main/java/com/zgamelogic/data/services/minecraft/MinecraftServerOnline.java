package com.zgamelogic.data.services.minecraft;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedList;

@Getter
@Setter
@ToString(callSuper = true)
public class MinecraftServerOnline extends MinecraftServer {
    private int online;
    private LinkedList<String> players;

    public MinecraftServerOnline(MinecraftServer minecraftServer, MinecraftServerPingData minecraftServerPingData) {
        setFilePath(minecraftServer.getFilePath());
        setStatus(minecraftServer.getStatus());
        setName(minecraftServer.getName());
        setServerProperties(minecraftServer.getServerProperties());
        setServerConfig(minecraftServer.getServerConfig());

        online = minecraftServerPingData.getOnlineCount();
        players = minecraftServerPingData.getPlayers();
    }
}
