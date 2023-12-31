package com.zgamelogic.data.services.minecraft;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedList;

@Getter
@AllArgsConstructor
@ToString
public class MinecraftServerPlayersPacket {
    private LinkedList<String> players;

    public int getPlayerCount(){
        return players.size();
    }
}
