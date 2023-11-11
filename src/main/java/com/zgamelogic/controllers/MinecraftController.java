package com.zgamelogic.controllers;

import com.zgamelogic.data.database.curseforge.CurseforgeProjectRepository;
import com.zgamelogic.data.services.minecraft.*;
import com.zgamelogic.services.MinecraftService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import static com.zgamelogic.data.Constants.MC_SERVER_ONLINE;

@Slf4j
@RestController
@PropertySource("File:msu.properties")
public class MinecraftController {

    @Value("${curseforge.token}")
    private String curseforgeToken;

    private final static File SERVER_DIR = new File("data/servers");
    private final HashMap<String, MinecraftServer> servers;
    private HashMap<String, LinkedList<MinecraftServerVersion>> serverVersions;

    private final WebSocketService webSocketService;
    private final CurseforgeProjectRepository curseforgeProjectRepository;

    @Autowired
    private MinecraftController(WebSocketService webSocketService, CurseforgeProjectRepository curseforgeProjectRepository){
        this.webSocketService = webSocketService;
        this.curseforgeProjectRepository = curseforgeProjectRepository;
        if(!SERVER_DIR.exists()) SERVER_DIR.mkdirs();
        serverVersions = new HashMap<>();
        servers = new HashMap<>();
        for(File server: SERVER_DIR.listFiles()){
            servers.put(server.getName(), new MinecraftServer(server, this::serverMessageAction));
        }
        log.info("Starting minecraft auto-start servers...");
        servers.values().stream().filter(mcServer -> mcServer.getServerConfig().isAutoStart())
                .forEach(MinecraftServer::startServer);
        updateServerVersions();
    }

    private void serverMessageAction(String name, String line){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("log", name, line);
        webSocketService.sendMessage("/server/message", msm);
    }

    @GetMapping("servers")
    private Collection<MinecraftServer> getServers(){
        return servers.values();
    }

    @GetMapping("/server/versions")
    public HashMap<String, LinkedList<MinecraftServerVersion>> getMinecraftServerVersions(){
        return serverVersions;
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

    @PostMapping("servers/update")
    private void updateServer(@RequestBody MinecraftServerUpdateCommand updateCommand){

    }

    @MessageMapping("/hello")
    @SendTo("/server/message")
    public MinecraftServer greeting(MinecraftWebsocketDataRequest message) {
        MinecraftServer server = servers.get(message.getServer());
        if(server.getStatus().equals(MC_SERVER_ONLINE)){
            MinecraftServerPingData pingData = MinecraftService.pingServer("localhost", Integer.parseInt(server.getServerProperties().get("server-port")));
            return new MinecraftServerOnline(server, pingData);
        }
        return server;
    }

    @PreDestroy
    private void preDestroy(){
        servers.values().stream().filter(mcServer -> mcServer.getStatus().equals("Online"))
                .forEach(MinecraftServer::stopServer);
    }

    @Scheduled(cron = "0 0 0 ? * *")
    private void updateServerVersions(){
        serverVersions = MinecraftService.getMinecraftServerVersions(curseforgeToken, curseforgeProjectRepository.findAll());
    }
}