package com.zgamelogic.controllers;

import com.zgamelogic.data.services.Greeting;
import com.zgamelogic.data.services.HelloMessage;
import com.zgamelogic.data.services.minecraft.MinecraftServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.LinkedList;

@Slf4j
@RestController
public class MinecraftController {

    private final static File SERVER_DIR = new File("data/servers");
    private LinkedList<MinecraftServer> servers;

    private MinecraftController(){
        if(!SERVER_DIR.exists()) SERVER_DIR.mkdirs();
        servers = new LinkedList<>();
        for(File server: SERVER_DIR.listFiles()){
            servers.add(new MinecraftServer(server));
        }
        log.info("Starting minecraft auto-start servers...");
        servers.stream().filter(mcServer -> mcServer.getServerConfig().isAutoStart())
                .forEach(MinecraftServer::startServer);
    }

    @GetMapping("servers")
    public LinkedList<MinecraftServer> getServers(){
        return servers;
    }

    @MessageMapping("/hello")
    @SendTo("/server/message")
    public Greeting greeting(HelloMessage message) {
        log.info("Got a client message");
        return new Greeting("Hello, " + message.getName() + "!");
    }
}