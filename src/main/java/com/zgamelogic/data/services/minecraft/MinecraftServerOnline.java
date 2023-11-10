package com.zgamelogic.data.services.minecraft;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;

@Getter
@Setter
public class MinecraftServerOnline extends MinecraftServer {
    private int online;
    private LinkedList<String> players;

}
