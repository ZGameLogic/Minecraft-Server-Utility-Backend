package com.zgamelogic.data.minecraft;

import java.util.LinkedList;

public interface MinecraftServerPlayerNotificationAction {
    void action(String server, String player, boolean joined, LinkedList<String> online);
}
