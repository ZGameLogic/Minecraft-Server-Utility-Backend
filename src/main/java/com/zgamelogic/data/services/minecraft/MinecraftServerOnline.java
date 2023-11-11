package com.zgamelogic.data.services.minecraft;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;

@Getter
@Setter
public class MinecraftServerOnline extends MinecraftServer {
    private int online;
    private LinkedList<String> players;
    private String log;

    public MinecraftServerOnline(MinecraftServer minecraftServer, MinecraftServerPingData minecraftServerPingData) {
        setFilePath(minecraftServer.getFilePath());
        setStatus(minecraftServer.getStatus());
        setName(minecraftServer.getName());
        setServerProperties(minecraftServer.getServerProperties());
        setServerConfig(minecraftServer.getServerConfig());

        online = minecraftServerPingData.getOnlineCount();
        players = minecraftServerPingData.getPlayers();
        loadLog();
    }

    private void loadLog(){
        File logFile = new File(getFilePath() + "/logs/latest.log");
        StringBuilder log = new StringBuilder();
        try {
            Scanner input = new Scanner(logFile);
            input.useDelimiter("\n");
            input.forEachRemaining(line -> log.append(line).append("\n"));
        } catch (FileNotFoundException ignored) {}
        this.log = log.toString();
    }
}
