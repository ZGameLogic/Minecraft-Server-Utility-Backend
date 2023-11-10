package com.zgamelogic.controllers;

import com.zgamelogic.data.services.Greeting;
import com.zgamelogic.data.services.HelloMessage;
import com.zgamelogic.data.services.minecraft.MinecraftServer;
import com.zgamelogic.data.services.minecraft.MinecraftServerStatusCommand;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

@Slf4j
@RestController
public class MinecraftController {

    private final static File SERVER_DIR = new File("data/servers");
    private final HashMap<String, MinecraftServer> servers;

    private MinecraftController(){
        if(!SERVER_DIR.exists()) SERVER_DIR.mkdirs();
        servers = new HashMap<>();
        for(File server: SERVER_DIR.listFiles()){
            servers.put(server.getName(), new MinecraftServer(server));
        }
        log.info("Starting minecraft auto-start servers...");
        servers.values().stream().filter(mcServer -> mcServer.getServerConfig().isAutoStart())
                .forEach(MinecraftServer::startServer);
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
    public Greeting greeting(HelloMessage message) {
        log.info("Got a client message");
        return new Greeting("Hello, " + message.getName() + "!");
    }

    @PreDestroy
    private void preDestroy(){
        servers.values().stream().filter(mcServer -> mcServer.getStatus().equals("Online"))
                .forEach(MinecraftServer::stopServer);
    }
}