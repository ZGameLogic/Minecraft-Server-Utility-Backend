package com.zgamelogic.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.database.curseforge.CurseforgeProject;
import com.zgamelogic.data.database.curseforge.CurseforgeProjectRepository;
import com.zgamelogic.data.services.curseforge.CurseforgeMod;
import com.zgamelogic.data.services.minecraft.*;
import com.zgamelogic.services.CurseforgeService;
import com.zgamelogic.services.MinecraftService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import static com.zgamelogic.data.Constants.MC_SERVER_ONLINE;
import static com.zgamelogic.services.MinecraftService.downloadServer;

@Slf4j
@RestController
@PropertySource("File:msu.properties")
public class MinecraftController {

    @Value("${curseforge.token}")
    private String curseforgeToken;

    private final static File SERVERS_DIR = new File("data/servers");
    private final HashMap<String, MinecraftServer> servers;
    private HashMap<String, HashMap<String, MinecraftServerVersion>> serverVersions;

    private final WebSocketService webSocketService;
    private final CurseforgeProjectRepository curseforgeProjectRepository;

    @Autowired
    private MinecraftController(WebSocketService webSocketService, CurseforgeProjectRepository curseforgeProjectRepository){
        this.webSocketService = webSocketService;
        this.curseforgeProjectRepository = curseforgeProjectRepository;
        if(!SERVERS_DIR.exists()) SERVERS_DIR.mkdirs();
        serverVersions = new HashMap<>();
        servers = new HashMap<>();
        for(File server: SERVERS_DIR.listFiles()){
            servers.put(server.getName(), new MinecraftServer(server, this::serverMessageAction, this::serverStatusAction));
        }
        log.info("Starting minecraft auto-start servers...");
        servers.values().stream().filter(mcServer -> mcServer.getServerConfig().isAutoStart())
                .forEach(MinecraftServer::startServer);
    }

    @PostConstruct
    private void postConstruct(){
        updateServerVersions();
    }

    private void serverMessageAction(String name, String line){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("log", name, line);
        webSocketService.sendMessage("/server/message", msm);
    }

    private void serverStatusAction(String name, String status){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("status", name, status);
        webSocketService.sendMessage("/server/message", msm);
    }

    @GetMapping("servers")
    private Collection<MinecraftServer> getServers(){
        return servers.values();
    }

    @GetMapping("server/ping/{server}")
    private Collection<MinecraftServerPingData> getServerPing(@PathVariable(required = false) String server){
        LinkedList<MinecraftServerPingData> data = new LinkedList<>();
        if(server != null){
            MinecraftServer minecraftServer = servers.get(server);
            if(minecraftServer.getStatus().equals(MC_SERVER_ONLINE)) {
                int port = Integer.parseInt(minecraftServer.getServerProperties().get("server-port"));
                String address = "zgamelogic.com";
                data.add(MinecraftService.pingServer(address, port));
            }
        } else {
            servers.forEach((key, minecraftServer) -> {
                if(minecraftServer.getStatus().equals(MC_SERVER_ONLINE)) {
                    int port = Integer.parseInt(minecraftServer.getServerProperties().get("server-port"));
                    String address = "zgamelogic.com";
                    data.add(MinecraftService.pingServer(address, port));
                }
            });
        }
        return data;
    }

    @GetMapping("server/log/{server}")
    private void getServerLog(@PathVariable String server){
        // TODO implement
    }

    @GetMapping("/server/versions")
    public HashMap<String, HashMap<String, MinecraftServerVersion>> getMinecraftServerVersions(){
        return serverVersions;
    }

    @PostMapping("server/command")
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

    @PostMapping("server/create")
    private void createServer(@RequestBody MinecraftServerCreationData data){
        MinecraftServerConfig config = new MinecraftServerConfig(data);
        File serverDir = new File(SERVERS_DIR + "/" + data.getName());
        serverDir.mkdirs();
        try {
            File eula = new File(serverDir.getPath() + "/eula.txt");
            PrintWriter eulaPW = new PrintWriter(eula);
            eulaPW.println("eula=true");
            eulaPW.flush();
            eulaPW.close();
            File props = new File(serverDir.getPath() + "/server.properties");
            PrintWriter propsPW = new PrintWriter(props);
            propsPW.println("server-port=" + data.getPort());
            propsPW.flush();
            propsPW.close();
            File configFile = new File(serverDir.getPath() + "/msu_config.json");
            ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
            ow.writeValue(configFile, config);
        } catch (IOException ignored) {}
        String download = serverVersions.get(data.getCategory()).get(data.getVersion()).getUrl();
        downloadServer(serverDir, download);
        servers.clear();
        for(File server: SERVERS_DIR.listFiles()){
            servers.put(server.getName(), new MinecraftServer(server, this::serverMessageAction, this::serverStatusAction));
        }
        if(config.isAutoStart()) servers.get(data.getName()).startServer();
    }

    @PostMapping("server/update")
    private void updateServer(@RequestBody MinecraftServerUpdateCommand updateCommand){
        servers.get(updateCommand.getServer()).updateServer(serverVersions.get(updateCommand.getCategory()).get(updateCommand.getVersion()).getUrl());
    }

    @GetMapping("curseforge/project")
    private CurseforgeMod getCurseforgeProject(@RequestBody CurseforgeProjectData data){
        return CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId());
    }

    @PostMapping("curseforge/project")
    private void addCurseforgeProject(@RequestBody CurseforgeProjectData data){
        CurseforgeMod project = CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId());
        curseforgeProjectRepository.save(new CurseforgeProject(data.getProjectId(), project.getName()));
        updateServerVersions();
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