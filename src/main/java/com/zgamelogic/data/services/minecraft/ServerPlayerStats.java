package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedList;

@Getter
public class ServerPlayerStats {

    private LinkedList stats;

    public ServerPlayerStats(HashMap<String, String> usernameCache, HashMap<String, JsonNode> userStats){

    }
}
