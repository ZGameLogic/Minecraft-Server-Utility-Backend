package com.zgamelogic.controllers;

import com.zgamelogic.data.services.minecraft.*;
import com.zgamelogic.services.MCServerService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import static com.zgamelogic.data.Constants.MC_SERVER_ONLINE;

@Slf4j
@RestController
public class MinecraftController {

    private final static File SERVER_DIR = new File("data/servers");
    private final HashMap<String, MinecraftServer> servers;

    private final WebSocketService webSocketService;

    @Autowired
    private MinecraftController(WebSocketService webSocketService){
        this.webSocketService = webSocketService;
        if(!SERVER_DIR.exists()) SERVER_DIR.mkdirs();
        servers = new HashMap<>();
        for(File server: SERVER_DIR.listFiles()){
            servers.put(server.getName(), new MinecraftServer(server, this::serverMessageAction));
        }
        log.info("Starting minecraft auto-start servers...");
        servers.values().stream().filter(mcServer -> mcServer.getServerConfig().isAutoStart())
                .forEach(MinecraftServer::startServer);
    }

    private void serverMessageAction(String name, String line){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("log", name, line);
        webSocketService.sendMessage("/server/message", msm);
    }

    @GetMapping("servers")
    private Collection<MinecraftServer> getServers(){
        return servers.values();
    }

    @PostMapping("servers/status")
    private void sendCommand(@RequestBody MinecraftServerStatusCommand command){
        if(!servers.containsKey(command.server())) return;
        switch (command.command()){
            case "restart":
                servers.get(command.server()).restartServer();
                break;
            case "stop":
                servers.get(command.server()).stopServer();
                break;
            case "start":
                servers.get(command.server()).startServer();
                break;
        }
    }

    @MessageMapping("/hello")
    @SendTo("/server/message")
    public MinecraftServer greeting(MinecraftWebsocketDataRequest message) {
        MinecraftServer server = servers.get(message.getServer());
        if(server.getStatus().equals(MC_SERVER_ONLINE)){
            MinecraftServerPingData pingData = MCServerService.pingServer("localhost", Integer.parseInt(server.getServerProperties().get("server-port")));
            return new MinecraftServerOnline(server, pingData);
        }
        return server;
    }

    @PreDestroy
    private void preDestroy(){
        servers.values().stream().filter(mcServer -> mcServer.getStatus().equals("Online"))
                .forEach(MinecraftServer::stopServer);
    }
}